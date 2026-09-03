/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Resumable HTTP file download used by {@code softwareUpdateHandler} to stage upgrade artifacts into
 * {@code .artifacts/}.
 * <p>
 * The implementation depends only on the JDK + slf4j (no registration-client / registration-services
 * types) so that in T2 it can be moved unchanged into the Java 11 {@code registration-launcher-common}
 * module and shared with {@code _launcher.jar}. Timeouts are passed in as parameters rather than read
 * from the services {@code ApplicationContext} for the same reason.
 * <p>
 * <b>Resume safety.</b> A partial download is only resumed when the server's validator (a strong
 * {@code ETag}, falling back to {@code Last-Modified}) captured when the partial was first written
 * still matches — enforced via an {@code If-Range} request and a {@code <file>.part.meta} sidecar.
 * If the resource changed (or the server omits a validator) the partial is discarded and the file is
 * re-downloaded from scratch, so bytes from two different resource versions can never be spliced
 * together. The completed transfer is verified against {@code Content-Length} before the staged
 * {@code .part} is atomically moved onto the final file.
 * <p>
 * <b>Integrity scope.</b> This class verifies transfer <i>completeness</i> (byte count) only, not
 * content <i>integrity</i>. Cryptographic verification of a downloaded artifact (SHA / HMAC against the
 * signed {@code MANIFEST.MF}) is deliberately performed downstream by the caller
 * ({@code softwareUpdateHandler} and the launcher's manifest verifier) before the artifact is placed
 * into use, so it is intentionally out of scope here.
 */
public final class ResumableDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResumableDownloader.class);
    private static final String PART_SUFFIX = ".part";
    private static final String META_SUFFIX = ".part.meta";
    private static final int HTTP_RANGE_NOT_SATISFIABLE = 416;
    private static final int BUFFER_SIZE = 8192;

    /** Report progress at most once per megabyte written, to keep UI callbacks cheap. */
    private static final long PROGRESS_REPORT_BYTES = 1024L * 1024L;

    private ResumableDownloader() {
        // utility class
    }

    /**
     * Downloads {@code url} into {@code targetDir/fileName} with resume support.
     * <p>
     * The content is staged into a {@code <fileName>.part} file. If a partial file (and its validator
     * sidecar) already exists from an interrupted attempt, the download resumes from the last received
     * byte using an {@code If-Range}-guarded HTTP {@code Range} request; if the server indicates the
     * resource has changed it transparently restarts from scratch. On successful completion the part
     * file is atomically moved onto the final file. If the download fails the part file is retained so
     * a subsequent call can resume.
     *
     * @param url            the source URL
     * @param targetDir      the directory the file should be written into (created if missing)
     * @param fileName       the final file name within {@code targetDir}
     * @param connectTimeout connection timeout in milliseconds
     * @param readTimeout    read timeout in milliseconds; must be positive (0 would mean infinite)
     * @throws IOException              if the download cannot be completed
     * @throws IllegalArgumentException if {@code fileName} is blank or contains a path separator or a
     *                                  {@code ..} sequence (guards against path-traversal writes outside
     *                                  {@code targetDir})
     */
    public static void download(String url, String targetDir, String fileName,
                                int connectTimeout, int readTimeout) throws IOException {
        download(url, targetDir, fileName, connectTimeout, readTimeout, NO_PROGRESS);
    }

    /**
     * Byte-level progress callback for a single artifact. Invoked on the download thread as the body
     * is streamed, so implementations must not touch UI state directly.
     */
    @FunctionalInterface
    public interface ProgressListener {
        /**
         * @param bytesDone  bytes of this artifact written so far, including any resumed prefix
         * @param totalBytes the artifact's full size, or a negative value when the server did not
         *                   supply a usable Content-Length. The FINAL callback of a successful download
         *                   always carries a real size: once the file is on disk its length is known,
         *                   even for a chunked transfer that was indeterminate throughout.
         */
        void onBytes(long bytesDone, long totalBytes);
    }

    /** Listener used by the progress-free overload. */
    private static final ProgressListener NO_PROGRESS = (done, total) -> {
        // no progress reporting requested
    };

    /**
     * As {@link #download(String, String, String, int, int)}, additionally reporting byte-level
     * progress for this artifact. Progress is reported against the artifact's full size, so a resumed
     * download starts from the bytes already on disk rather than from zero.
     */
    /**
     * Rejects non-positive timeouts. {@code HttpURLConnection} reads 0 as an INFINITE timeout, so a
     * stalled upgrade server would hang the calling thread forever -- the caller is left with no way to
     * notice or recover. Callers routing through {@code SoftwareUpdateUtil.getTimeout} already get a
     * positive value; this makes the guarantee the downloader's own rather than its callers'.
     * Mirrors the launcher-side {@code ResumableDownloader.requirePositiveTimeouts}.
     */
    private static void requirePositiveTimeouts(int connectTimeout, int readTimeout) {
        if (connectTimeout <= 0) {
            throw new IllegalArgumentException("connectTimeout must be positive, was " + connectTimeout);
        }
        if (readTimeout <= 0) {
            throw new IllegalArgumentException("readTimeout must be positive, was " + readTimeout);
        }
    }

    public static void download(String url, String targetDir, String fileName,
                                int connectTimeout, int readTimeout,
                                ProgressListener progressListener) throws IOException {
        LOGGER.info("Resumable download invoked, url : {}, target : {}/{}", url, targetDir, fileName);
        requirePositiveTimeouts(connectTimeout, readTimeout);
        File dir = new File(targetDir);
        if (!dir.exists() && !dir.mkdirs() && !dir.exists()) {
            throw new IOException("Unable to create target directory " + dir.getAbsolutePath());
        }
        Artifact artifact = new Artifact(dir, fileName);

        // No monotonicity guard is needed around progressListener. A restart is only triggered by
        // attemptDownload returning false, which happens on the 206-wrong-offset and 416-discard paths --
        // both of which return BEFORE writeBody, the only source of progress callbacks. So the second
        // attempt can never follow a report, and progress cannot walk backwards.
        //
        // At most two attempts: a resume attempt, and (only when the server signals the partial is
        // stale / unusable) one fresh restart with the stale part discarded.
        boolean allowResume = true;
        for (int attempt = 0; attempt < 2; attempt++) {
            if (attemptDownload(url, artifact, allowResume, connectTimeout, readTimeout, progressListener)) {
                // Guarantee a final 100% report on every success path. The 416 "the part is already the
                // whole file" branch finalizes without streaming a body, so it would otherwise complete
                // having reported nothing at all. Guarded on a real size: a zero-length artifact would
                // otherwise report (0, 0), which callers read as "length unknown" and would snap an
                // otherwise-finished bar back to indeterminate.
                long finalSize = artifact.target.length();
                if (finalSize > 0) {
                    progressListener.onBytes(finalSize, finalSize);
                }
                return;
            }
            allowResume = false; // the partial was discarded; the next attempt starts from scratch
        }
        throw new IOException("Failed to download " + url + " after restart");
    }

    /**
     * Performs a single download attempt.
     *
     * @return {@code true} if the file was completed and finalized; {@code false} if the partial was
     *         found stale/unusable and discarded, so the caller should restart from scratch.
     */
    private static boolean attemptDownload(String url, Artifact artifact, boolean allowResume,
                                           int connectTimeout, int readTimeout,
                                           ProgressListener progressListener) throws IOException {
        String validator = allowResume ? resumeValidator(artifact) : null;
        long existing = (validator != null) ? artifact.part.length() : 0L;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        // Force an uncompressed transfer: with a byte Range request, transparent gzip anywhere in the
        // path would make the Content-Length (compressed) and the byte offsets disagree with the bytes
        // written to .part, breaking both the completeness check and any subsequent resume.
        connection.setRequestProperty("Accept-Encoding", "identity");
        if (existing > 0) {
            connection.setRequestProperty("Range", "bytes=" + existing + "-");
            connection.setRequestProperty("If-Range", validator);
            LOGGER.info("Resuming {} from byte {} (If-Range guarded)", artifact.name, existing);
        }
        try {
            connection.connect();
            int status = connection.getResponseCode();
            boolean append;
            if (status == HttpURLConnection.HTTP_PARTIAL) {            // 206 - server honoured Range
                long start = parseContentRangeStart(connection.getHeaderField("Content-Range"));
                if (start != existing) {
                    LOGGER.warn("206 for {} started at byte {} but expected {}; discarding and restarting",
                            artifact.name, start, existing);
                    artifact.discard();
                    return false;
                }
                append = true;
            } else if (status == HttpURLConnection.HTTP_OK) {          // 200 - fresh body (or If-Range mismatch)
                append = false;
                existing = 0L;
                // Discard any stale partial BEFORE persisting the new validator. Writing the new
                // validator while old bytes still sit in .part would, if the process died before
                // writeBody truncates them (e.g. ensureSpaceForWrite throws), leave a new validator
                // paired with stale bytes — which the next resume would splice onto the new resource
                // via If-Range. Deleting first keeps .part and .meta in sync (and frees the old bytes
                // so the disk-space pre-check below sees them as available).
                Files.deleteIfExists(artifact.part.toPath());
                // Persist the validator so a later interrupted attempt can resume this exact resource.
                writeValidator(artifact.meta, extractValidator(connection));
            } else if (status == HTTP_RANGE_NOT_SATISFIABLE) {         // 416 - part is at/over server size
                return handleRangeNotSatisfiable(connection, artifact);
            } else {
                throw new IOException("Unexpected HTTP status " + status + " while downloading " + url);
            }

            long contentLength = connection.getContentLengthLong();
            ensureSpaceForWrite(artifact.dir, artifact.part, contentLength, append);
            // On a 206 the Content-Length covers only the remaining range, so the artifact's full size
            // is what is already on disk plus what is still to come. Negative stays negative: an unknown
            // length must be reported as unknown rather than mistaken for a complete file.
            long artifactTotal = (contentLength < 0) ? -1L : (append ? existing + contentLength : contentLength);
            writeBody(connection, artifact.part, append, existing, artifactTotal, progressListener);
            verifyComplete(artifact, contentLength, append, existing);
            artifact.finalizeOnto();
            LOGGER.info("Resumable download completed : {}", artifact.name);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to download {} (partial file retained for resume)", url, e);
            throw e;
        } finally {
            connection.disconnect();
        }
    }

    /** Handles a 416 response: finalize if the part is already the complete file, else discard + restart. */
    private static boolean handleRangeNotSatisfiable(HttpURLConnection connection, Artifact artifact)
            throws IOException {
        long total = parseContentRangeTotal(connection.getHeaderField("Content-Range"));
        long partLength = artifact.part.exists() ? artifact.part.length() : 0L;
        if (total >= 0 && partLength == total) {
            LOGGER.info("Range not satisfiable for {} and part matches server size; finalizing", artifact.name);
            artifact.finalizeOnto();
            return true;
        }
        LOGGER.warn("416 for {} but local part ({} bytes) != server total ({}); restarting fresh",
                artifact.name, partLength, total);
        artifact.discard();
        return false;
    }

    /**
     * Returns the validator stored for a resumable partial, or {@code null} if the partial cannot be
     * safely resumed (no partial, or no stored validator) — in which case any stale partial is removed.
     */
    private static String resumeValidator(Artifact artifact) throws IOException {
        if (!artifact.part.exists()) {
            return null;
        }
        String validator = readValidator(artifact.meta);
        if (validator == null) {
            LOGGER.warn("Partial {} has no stored validator; cannot safely resume, restarting", artifact.name);
            Files.deleteIfExists(artifact.part.toPath());
        }
        return validator;
    }

    /** Streams the response body into the part file, appending from {@code existing} or overwriting. */
    private static void writeBody(HttpURLConnection connection, File partFile, boolean append, long existing,
                                  long artifactTotal, ProgressListener progressListener) throws IOException {
        try (InputStream in = connection.getInputStream();
             RandomAccessFile out = new RandomAccessFile(partFile, "rw")) {
            long written = append ? existing : 0L;
            if (append) {
                out.seek(existing);
            } else {
                out.setLength(0);
            }
            progressListener.onBytes(written, artifactTotal);
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            long sinceLastReport = 0L;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                written += read;
                sinceLastReport += read;
                // Throttle: a ~200MB artifact is millions of buffer reads, and a UI callback per read
                // would swamp the FX event queue that this progress is meant to keep responsive.
                if (sinceLastReport >= PROGRESS_REPORT_BYTES) {
                    progressListener.onBytes(written, artifactTotal);
                    sinceLastReport = 0L;
                }
            }
            progressListener.onBytes(written, artifactTotal);
        }
    }

    /**
     * Detects a truncated transfer: when the length is known, what landed on disk must match it. A
     * premature EOF (server/connection closed early) can otherwise end the read loop cleanly and
     * finalize a short, corrupt file. The part is retained (this throws) so it can resume.
     */
    private static void verifyComplete(Artifact artifact, long contentLength, boolean append, long existing)
            throws IOException {
        if (contentLength < 0) {
            return;
        }
        long expected = (append ? existing : 0L) + contentLength;
        if (artifact.part.length() != expected) {
            throw new IOException("Incomplete download of " + artifact.name + ": expected " + expected
                    + " bytes but got " + artifact.part.length() + " (partial retained for resume)");
        }
    }

    /**
     * Disk-space guard for the write about to happen. On a full (200) restart the existing part will be
     * truncated, so its bytes are added back to the available figure. When the content length is
     * unknown (e.g. chunked transfer with no {@code Content-Length}) the pre-check cannot be performed
     * and is skipped with a warning (rather than silently passing a zero requirement).
     */
    private static void ensureSpaceForWrite(File dir, File partFile, long contentLength, boolean append)
            throws IOException {
        if (contentLength < 0) {
            LOGGER.warn("Content-Length unknown for {}; skipping disk-space pre-check", dir.getAbsolutePath());
            return;
        }
        long freeable = append ? 0L : (partFile.exists() ? partFile.length() : 0L);
        long usableSpace = dir.getUsableSpace() + freeable;
        if (contentLength > usableSpace) {
            LOGGER.error("Insufficient disk space at {} : required {} bytes, available {} bytes",
                    dir.getAbsolutePath(), contentLength, usableSpace);
            throw new IOException("Not enough space available to download. Required: " + contentLength
                    + " bytes, Available: " + usableSpace + " bytes");
        }
    }

    /**
     * Picks a validator usable with {@code If-Range}: a strong {@code ETag} if present, else
     * {@code Last-Modified}. Weak ETags ({@code W/...}) are not valid for byte-range validation per
     * RFC 7233 and are ignored. Returns {@code null} when the server provides neither — in which case
     * the download cannot be safely resumed and is restarted instead.
     */
    private static String extractValidator(HttpURLConnection connection) {
        String etag = connection.getHeaderField("ETag");
        if (etag != null) {
            etag = etag.trim();
            if (!etag.isEmpty() && !etag.startsWith("W/")) {
                return etag;
            }
        }
        String lastModified = connection.getHeaderField("Last-Modified");
        if (lastModified != null && !lastModified.trim().isEmpty()) {
            return lastModified.trim();
        }
        return null;
    }

    private static String readValidator(File metaFile) {
        if (!metaFile.exists()) {
            return null;
        }
        try {
            String value = new String(Files.readAllBytes(metaFile.toPath()), StandardCharsets.UTF_8).trim();
            return value.isEmpty() ? null : value;
        } catch (IOException e) {
            LOGGER.warn("Could not read validator {} : {}", metaFile, e.getMessage());
            return null;
        }
    }

    private static void writeValidator(File metaFile, String validator) {
        try {
            if (validator == null) {
                Files.deleteIfExists(metaFile.toPath());
            } else {
                Files.write(metaFile.toPath(), validator.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            LOGGER.warn("Could not persist validator {} : {}", metaFile, e.getMessage());
        }
    }

    /** Parses the start offset from a {@code Content-Range: bytes <start>-<end>/<total>} header; -1 if unknown. */
    private static long parseContentRangeStart(String contentRange) {
        if (contentRange == null) {
            return -1;
        }
        String value = contentRange.trim();
        int space = value.indexOf(' ');
        if (space >= 0) {
            value = value.substring(space + 1);
        }
        int dash = value.indexOf('-');
        if (dash <= 0) {
            return -1;
        }
        try {
            return Long.parseLong(value.substring(0, dash).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Parses the total size from a {@code Content-Range: bytes <range>/<total>} header; -1 if unknown. */
    private static long parseContentRangeTotal(String contentRange) {
        if (contentRange == null) {
            return -1;
        }
        int slash = contentRange.lastIndexOf('/');
        if (slash < 0 || slash == contentRange.length() - 1) {
            return -1;
        }
        String total = contentRange.substring(slash + 1).trim();
        if ("*".equals(total)) {
            return -1;
        }
        try {
            return Long.parseLong(total);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Guards against starting a write that cannot fit on the target's partition. Checks the usable
     * space of the actual target directory's partition rather than the file-system root, which is
     * important on Windows where the install drive may differ from {@code C:\}.
     *
     * @param targetDir     the directory the data will be written to
     * @param requiredBytes the number of bytes about to be written
     * @throws IOException if there is insufficient usable space
     */
    public static void ensureSpace(File targetDir, long requiredBytes) throws IOException {
        File dir = (targetDir != null && targetDir.exists()) ? targetDir : new File(".");
        long usableSpace = dir.getUsableSpace();
        if (requiredBytes > usableSpace) {
            LOGGER.error("Insufficient disk space at {} : required {} bytes, available {} bytes",
                    dir.getAbsolutePath(), requiredBytes, usableSpace);
            throw new IOException("Not enough space available to download. Required: " + requiredBytes
                    + " bytes, Available: " + usableSpace + " bytes");
        }
    }

    /** The set of files involved in staging one download: the final file, its {@code .part}, and its validator sidecar. */
    private static final class Artifact {
        private final File dir;
        private final String name;
        private final File target;
        private final File part;
        private final File meta;

        private Artifact(File dir, String name) {
            if (name == null || name.isEmpty()
                    || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0
                    || name.contains("..")) {
                throw new IllegalArgumentException(
                        "Illegal download file name (path traversal attempt?): " + name);
            }
            this.dir = dir;
            this.name = name;
            this.target = new File(dir, name);
            this.part = new File(dir, name + PART_SUFFIX);
            this.meta = new File(dir, name + META_SUFFIX);
        }

        /** Discards a stale partial download and its validator sidecar. */
        private void discard() throws IOException {
            Files.deleteIfExists(part.toPath());
            Files.deleteIfExists(meta.toPath());
        }

        /**
         * Atomically moves the completed part file onto the target file and removes the validator
         * sidecar. Falls back to a non-atomic replace when the file system has no atomic move.
         */
        private void finalizeOnto() throws IOException {
            try {
                Files.move(part.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveUnsupported) {
                Files.move(part.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            Files.deleteIfExists(meta.toPath());
        }
    }
}
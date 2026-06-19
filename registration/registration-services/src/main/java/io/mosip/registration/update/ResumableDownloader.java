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
 */
public final class ResumableDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResumableDownloader.class);
    private static final String PART_SUFFIX = ".part";
    private static final String META_SUFFIX = ".part.meta";
    private static final int HTTP_RANGE_NOT_SATISFIABLE = 416;
    private static final int BUFFER_SIZE = 8192;

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
     * @param readTimeout    read timeout in milliseconds (0 = infinite)
     * @throws IOException              if the download cannot be completed
     * @throws IllegalArgumentException if {@code fileName} is blank or contains a path separator or a
     *                                  {@code ..} sequence (guards against path-traversal writes outside
     *                                  {@code targetDir})
     */
    public static void download(String url, String targetDir, String fileName,
                                int connectTimeout, int readTimeout) throws IOException {
        LOGGER.info("Resumable download invoked, url : {}, target : {}/{}", url, targetDir, fileName);
        File dir = new File(targetDir);
        if (!dir.exists() && !dir.mkdirs() && !dir.exists()) {
            throw new IOException("Unable to create target directory " + dir.getAbsolutePath());
        }
        Artifact artifact = new Artifact(dir, fileName);

        // At most two attempts: a resume attempt, and (only when the server signals the partial is
        // stale / unusable) one fresh restart with the stale part discarded.
        boolean allowResume = true;
        for (int attempt = 0; attempt < 2; attempt++) {
            if (attemptDownload(url, artifact, allowResume, connectTimeout, readTimeout)) {
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
                                           int connectTimeout, int readTimeout) throws IOException {
        String validator = allowResume ? resumeValidator(artifact) : null;
        long existing = (validator != null) ? artifact.part.length() : 0L;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
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
                // Persist the validator so a later interrupted attempt can resume this exact resource.
                writeValidator(artifact.meta, extractValidator(connection));
            } else if (status == HTTP_RANGE_NOT_SATISFIABLE) {         // 416 - part is at/over server size
                return handleRangeNotSatisfiable(connection, artifact);
            } else {
                throw new IOException("Unexpected HTTP status " + status + " while downloading " + url);
            }

            long contentLength = connection.getContentLengthLong();
            ensureSpaceForWrite(artifact.dir, artifact.part, contentLength, append);
            writeBody(connection, artifact.part, append, existing);
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
    private static void writeBody(HttpURLConnection connection, File partFile, boolean append, long existing)
            throws IOException {
        try (InputStream in = connection.getInputStream();
             RandomAccessFile out = new RandomAccessFile(partFile, "rw")) {
            if (append) {
                out.seek(existing);
            } else {
                out.setLength(0);
            }
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
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
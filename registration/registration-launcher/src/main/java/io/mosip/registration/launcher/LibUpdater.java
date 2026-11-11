/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher;

import io.mosip.registration.launcher.common.DownloadProgressListener;
import io.mosip.registration.launcher.common.LauncherLog;
import io.mosip.registration.launcher.common.ManifestVerifier;
import io.mosip.registration.launcher.common.OperatorAlertListener;
import io.mosip.registration.launcher.common.ResumableDownloader;
import io.mosip.registration.launcher.common.SignatureVerifier;
import io.mosip.registration.launcher.common.ZipExtractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Step 5 of the launcher (design doc): the lib-only update path taken when the JRE is already 21+
 * and the manifest versions differ. Downloads the new {@code lib/MANIFEST.MF}(+{@code .sig}) and
 * {@code lib.zip} into {@code .TEMP/}, verifies them, and leaves the staged update for {@code run.bat}
 * to copy into {@code lib/} on the next restart.
 * <p>
 * <b>Design note</b> (see {@code design/registration/registration-upgrade.md}, step 5): the design says
 * "verify lib.zip hash against an entry in MANIFEST.MF", but the dual-manifest model states
 * {@code lib/MANIFEST.MF} carries <i>per-file</i> hashes and no {@code lib.zip} entry. This implements
 * the consistent reading: verify the manifest's signature, then verify each <i>extracted</i> file
 * against its per-file hash.
 * <p>
 * The per-file check resolves each manifest entry by its {@code .TEMP/}-relative path, so it honours
 * whatever layout the signed {@code lib/MANIFEST.MF} declares — flat jars at the root, or jars nested
 * under sub-directories. The only requirement is that the manifest's entry names match the extracted
 * layout; a disagreement (e.g. a build that ships a nested jar but lists it by a bare name) correctly
 * fails closed as {@link LibUpdateResult#VERIFY_FAILED}.
 * <p>
 * <b>{@code .TEMP/} only ever appears complete.</b> {@code run.bat} copies whatever is in {@code .TEMP/}
 * into {@code lib/} on the next start, and the launcher then trusts a signature-valid
 * {@code lib/MANIFEST.MF} whose version matches the root one. So nothing is written to {@code .TEMP/}
 * until the whole update has been downloaded, unpacked and verified: the manifest, its signature and
 * {@code lib.zip} are downloaded into the {@code .zipstage} sibling, unpacked into the {@code .extract}
 * sibling, and that tree is renamed to {@code .TEMP/} as the last step. An update interrupted at any
 * earlier point leaves no {@code .TEMP/}, the versions still differ on the next start, and the update
 * is re-entered, resuming {@code lib.zip} from its {@code .part}.
 */
public final class LibUpdater {

    private static final LauncherLog LOGGER = LauncherLog.get(LibUpdater.class);
    private static final String MANIFEST = "MANIFEST.MF";
    private static final String MANIFEST_SIG = "MANIFEST.MF.sig";
    private static final String LIB_ZIP = "lib.zip";
    // Upper bounds for the metadata bodies buffered fully into memory before verification, so a
    // compromised/misconfigured server cannot force an unbounded allocation via an oversized response.
    // A detached SHA256withRSA signature is tiny (RSA-4096 -> 512 bytes); lib/MANIFEST.MF holds one
    // per-file hash line per jar (KBs in practice) — 1 MiB is comfortably generous for both.
    private static final long MAX_SIGNATURE_BYTES = 1024L;
    private static final long MAX_MANIFEST_BYTES = 1024L * 1024L;

    /**
     * Control files legitimately present in the staged lib but not manifest entries. {@code lib.zip}
     * is intentionally NOT listed: it is downloaded to a sibling staging dir (never into the unpacked
     * tree), so a {@code lib.zip} entry unpacked from the archive is an unexpected file and correctly
     * rejected.
     */
    static final Set<String> CONTROL_FILES = new HashSet<>(Arrays.asList(MANIFEST, MANIFEST_SIG));

    private LibUpdater() {
        // utility class
    }

    /**
     * Performs the step-5 lib update.
     *
     * @param libManifestUrl    URL of the new {@code lib/MANIFEST.MF}
     * @param libManifestSigUrl URL of its detached {@code lib/MANIFEST.MF.sig}
     * @param libZipUrl         URL of {@code lib.zip}
     * @param tempDir           the {@code .TEMP/} staging directory
     * @param trustedKey        public key from the embedded {@code provider.pem}
     * @param connectTimeout    connection timeout (ms; must be positive)
     * @param readTimeout       read timeout (ms; must be positive)
     * @return the outcome (see {@link LibUpdateResult})
     * @throws IOException              if a download or extraction fails irrecoverably
     * @throws IllegalArgumentException if {@code connectTimeout} or {@code readTimeout} is not positive
     */
    public static LibUpdateResult update(String libManifestUrl, String libManifestSigUrl, String libZipUrl,
                                         File tempDir, PublicKey trustedKey,
                                         int connectTimeout, int readTimeout) throws IOException {
        return update(libManifestUrl, libManifestSigUrl, libZipUrl, tempDir, trustedKey,
                connectTimeout, readTimeout, null);
    }

    /**
     * As {@link #update(String, String, String, File, PublicKey, int, int)}, additionally reporting the
     * {@code lib.zip} download's byte progress to {@code progress} (may be {@code null}). Only the
     * {@code lib.zip} transfer is reported — the manifest and its signature are tiny.
     */
    public static LibUpdateResult update(String libManifestUrl, String libManifestSigUrl, String libZipUrl,
                                         File tempDir, PublicKey trustedKey,
                                         int connectTimeout, int readTimeout,
                                         DownloadProgressListener progress) throws IOException {
        return update(libManifestUrl, libManifestSigUrl, libZipUrl, tempDir, trustedKey,
                connectTimeout, readTimeout, progress, null);
    }

    /**
     * As {@link #update(String, String, String, File, PublicKey, int, int, DownloadProgressListener)},
     * additionally telling {@code status} (may be {@code null}) which step is running, so the operator
     * watching the progress window can see the update moving rather than a single unchanging line. During
     * the {@code lib.zip} download the line also carries the megabytes received, which keeps visibly
     * advancing on a slow link where a whole percent can take several seconds.
     */
    public static LibUpdateResult update(String libManifestUrl, String libManifestSigUrl, String libZipUrl,
                                         File tempDir, PublicKey trustedKey,
                                         int connectTimeout, int readTimeout,
                                         DownloadProgressListener progress,
                                         OperatorAlertListener status) throws IOException {
        // Reject invalid timeouts at this entry point (fail fast) rather than letting a 0/infinite
        // value reach the per-file downloads below and hang the launcher.
        ResumableDownloader.requirePositiveTimeouts(connectTimeout, readTimeout);

        // Everything is staged in siblings of .TEMP/ (see the class note): .zipstage holds the downloads,
        // .extract the unpacked tree. getAbsoluteFile() guards against a relative .TEMP whose
        // getParentFile() would otherwise be null.
        File stagingParent = tempDir.getAbsoluteFile().getParentFile();
        File zipStagingDir = new File(stagingParent, tempDir.getName() + ".zipstage");
        File extractDir = new File(stagingParent, tempDir.getName() + ".extract");

        // 1. download the new lib manifest and its detached signature
        ResumableDownloader.download(libManifestUrl, zipStagingDir.getPath(), MANIFEST, connectTimeout, readTimeout);
        ResumableDownloader.download(libManifestSigUrl, zipStagingDir.getPath(), MANIFEST_SIG, connectTimeout, readTimeout);

        // 2. verify the manifest signature; on failure do not download lib.zip (Case B)
        File manifestFile = new File(zipStagingDir, MANIFEST);
        File signatureFile = new File(zipStagingDir, MANIFEST_SIG);
        byte[] manifestBytes = readCapped(manifestFile, MAX_MANIFEST_BYTES);
        byte[] signatureBytes = readCapped(signatureFile, MAX_SIGNATURE_BYTES);

        // A truncated download or an HTML/error body is a network/server problem, not a tamper —
        // detect it here so it surfaces as a plain failure rather than a misleading "signature
        // invalid" security alert. Keep the verified manifest in memory so the integrity check below
        // cannot be subverted by a MANIFEST.MF unpacked from lib.zip.
        Manifest trustedManifest = parseDownloadedManifest(manifestBytes, signatureBytes);

        if (!SignatureVerifier.verify(manifestBytes, signatureBytes, trustedKey)) {
            LOGGER.error("lib/MANIFEST.MF signature is invalid — aborting lib update");
            return LibUpdateResult.ABORT_INVALID_SIGNATURE;
        }

        // 3. resumable download of lib.zip into the staging dir — never into the tree it is unpacked to.
        //    Extracting an archive into the directory that also holds it lets a crafted entry named
        //    "lib.zip" overwrite the archive while it is still being read (and, as a former control
        //    file, slip past the allowlist). Keeping them apart closes that self-overwrite window; a
        //    stray "lib.zip" entry lands in the unpacked tree and is rejected as unexpected below.
        status(status, "Downloading the update…");
        ResumableDownloader.download(libZipUrl, zipStagingDir.getPath(), LIB_ZIP, connectTimeout, readTimeout,
                withDownloadStatus(progress, status));

        // Once the download returns, lib.zip is complete and no longer needs its resumable .part, so we
        // always drop the staging dir after extraction — even if extraction fails. An interrupted
        // download throws above (before this try), leaving the staging dir + .part intact so the
        // operator retry resumes instead of re-fetching from the start.
        try {
            // 4. unpack into a fresh tree: anything left there by an earlier attempt (a cut-off unzip,
            //    jars of another version) would otherwise survive and trip the allowlist below.
            deleteTree(extractDir);
            status(status, "Unpacking the update…");
            ZipExtractor.extract(new File(zipStagingDir, LIB_ZIP), extractDir);
        } catch (IOException | RuntimeException e) {
            deleteTree(extractDir);
            throw e;
        } finally {
            deleteTree(zipStagingDir);
        }

        // lib.zip may ship its own MANIFEST.MF(+sig); write the signature-verified bytes over them so the
        // manifest that run.bat copies into lib/ is the trusted one, not the (unverified) archived copy.
        Files.write(new File(extractDir, MANIFEST).toPath(), manifestBytes);
        Files.write(new File(extractDir, MANIFEST_SIG).toPath(), signatureBytes);

        // 5. verify each extracted file against the verified manifest, and reject any extra file the
        //    manifest does not list (allowlist).
        status(status, "Verifying the update files…");
        List<String> mismatched = ManifestVerifier.findMismatchedFiles(trustedManifest, extractDir);
        List<String> unexpected = ManifestVerifier.findUnexpectedFiles(trustedManifest, extractDir, CONTROL_FILES);
        if (!mismatched.isEmpty() || !unexpected.isEmpty()) {
            LOGGER.error("Extracted lib failed integrity check — mismatched: {}, unexpected: {}", mismatched, unexpected);
            deleteTree(extractDir);
            return LibUpdateResult.VERIFY_FAILED;
        }

        // 6. publish: only now does .TEMP/ appear, complete and verified, for run.bat to apply.
        deleteTree(tempDir);
        Files.move(extractDir.toPath(), tempDir.toPath());
        LOGGER.info("lib update staged in {} — restart required to apply", tempDir);
        return LibUpdateResult.READY_RESTART;
    }

    /**
     * Wraps the caller's byte-progress listener so each report also refreshes the status line with the
     * megabytes received. Called at most once per whole percent (see {@code ResumableDownloader}), so
     * the window is not flooded. Returns the caller's listener unchanged when there is no status sink.
     */
    private static DownloadProgressListener withDownloadStatus(DownloadProgressListener progress,
                                                               OperatorAlertListener status) {
        if (status == null) {
            return progress;
        }
        return (done, total) -> {
            if (progress != null) {
                progress.onProgress(done, total);
            }
            if (total > 0) {
                status(status, "Downloading the update (" + megabytes(done) + " of " + megabytes(total) + " MB)…");
            }
        };
    }

    /** Whole megabytes (1024 x 1024 bytes, as Windows Explorer shows them), rounded down. */
    static long megabytes(long bytes) {
        return bytes / (1024L * 1024L);
    }

    /**
     * Shows an operator-facing step line when a listener is wired, without logging it: the steps are
     * already logged in detail, and the per-percent download lines would flood {@code launcher.log}.
     * Never fails the update.
     */
    private static void status(OperatorAlertListener status, String message) {
        if (status == null) {
            return;
        }
        try {
            status.onAlert(message);
        } catch (RuntimeException e) {
            // A status line must never break the update it is reporting on.
            LOGGER.warn("Could not show the status line ({})", e.getMessage());
        }
    }

    /**
     * Reads {@code file} fully into memory but only after checking its on-disk size against
     * {@code maxBytes}, so an oversized (or empty) body — e.g. an HTML error page or a hostile
     * unbounded response — is rejected as a network/server error before it is buffered, rather than
     * driving an unbounded allocation. Framed as an {@link IOException} (not a tamper alert) to match
     * {@link #parseDownloadedManifest}: the signature check below is the actual integrity gate.
     */
    private static byte[] readCapped(File file, long maxBytes) throws IOException {
        long length = file.length();
        if (length == 0L || length > maxBytes) {
            throw new IOException("Downloaded " + file.getName() + " has unexpected size " + length
                    + " bytes (expected 1.." + maxBytes + "; likely a network/server error)");
        }
        return Files.readAllBytes(file.toPath());
    }

    /**
     * Parses the downloaded manifest, treating an empty/unparseable/version-less body as a download or
     * server error ({@link IOException}) rather than a signature mismatch — so the operator is not
     * shown a misleading security alert for what is actually a network/server problem.
     */
    private static Manifest parseDownloadedManifest(byte[] manifestBytes, byte[] signatureBytes) throws IOException {
        if (manifestBytes.length == 0 || signatureBytes.length == 0) {
            throw new IOException("Downloaded manifest or signature is empty (likely a network/server error)");
        }
        Manifest manifest;
        try {
            manifest = ManifestVerifier.parse(manifestBytes);
        } catch (IOException e) {
            throw new IOException("Downloaded manifest is not a valid manifest (likely a network/server error)", e);
        }
        if (manifest.getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION) == null) {
            throw new IOException("Downloaded manifest is malformed (likely a network/server error)");
        }
        return manifest;
    }

    /**
     * Recursively deletes {@code root} (a file, directory tree, or symlink) without following symlinks.
     * A non-existent {@code root} is a no-op.
     */
    private static void deleteTree(File root) throws IOException {
        try {
            Files.walkFileTree(root.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(path);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (NoSuchFileException e) {
            // already gone — nothing to delete
        }
    }
}

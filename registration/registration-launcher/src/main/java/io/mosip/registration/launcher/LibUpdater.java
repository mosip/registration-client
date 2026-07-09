/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher;

import io.mosip.registration.launcher.common.ManifestVerifier;
import io.mosip.registration.launcher.common.ResumableDownloader;
import io.mosip.registration.launcher.common.SignatureVerifier;
import io.mosip.registration.launcher.common.ZipExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
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
 * <b>Design note (recorded in upgrade-implementation-spec.md):</b> step 5 says "verify lib.zip hash
 * against an entry in MANIFEST.MF", but the dual-manifest model states {@code lib/MANIFEST.MF} carries
 * <i>per-file</i> hashes and no {@code lib.zip} entry. This implements the consistent reading: verify
 * the manifest's signature, then verify each <i>extracted</i> file against its per-file hash.
 * <p>
 * The per-file check resolves each manifest entry by its {@code .TEMP/}-relative path, so it honours
 * whatever layout the signed {@code lib/MANIFEST.MF} declares — flat jars at the root, or jars nested
 * under sub-directories. The only requirement is that the manifest's entry names match the extracted
 * layout; a disagreement (e.g. a build that ships a nested jar but lists it by a bare name) correctly
 * fails closed as {@link LibUpdateResult#VERIFY_FAILED}.
 */
public final class LibUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibUpdater.class);
    private static final String MANIFEST = "MANIFEST.MF";
    private static final String MANIFEST_SIG = "MANIFEST.MF.sig";
    private static final String LIB_ZIP = "lib.zip";
    // Upper bounds for the metadata bodies buffered fully into memory before verification, so a
    // compromised/misconfigured server cannot force an unbounded allocation via an oversized response.
    // A detached SHA256withRSA signature is tiny (RSA-4096 -> 512 bytes); lib/MANIFEST.MF holds one
    // per-file hash line per jar (KBs in practice) — 1 MiB is comfortably generous for both.
    private static final long MAX_SIGNATURE_BYTES = 1024L;
    private static final long MAX_MANIFEST_BYTES = 1024L * 1024L;

    /** Control files legitimately present in the staging dir but not manifest entries. */
    static final Set<String> CONTROL_FILES = new HashSet<>(Arrays.asList(MANIFEST, MANIFEST_SIG, LIB_ZIP));

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
        // Reject invalid timeouts at this entry point (fail fast) rather than letting a 0/infinite
        // value reach the per-file downloads below and hang the launcher.
        ResumableDownloader.requirePositiveTimeouts(connectTimeout, readTimeout);
        Files.createDirectories(tempDir.toPath());

        // 1. download the new lib manifest and its detached signature
        ResumableDownloader.download(libManifestUrl, tempDir.getPath(), MANIFEST, connectTimeout, readTimeout);
        ResumableDownloader.download(libManifestSigUrl, tempDir.getPath(), MANIFEST_SIG, connectTimeout, readTimeout);

        // 2. verify the manifest signature; on failure do not download lib.zip (Case B)
        File manifestFile = new File(tempDir, MANIFEST);
        File signatureFile = new File(tempDir, MANIFEST_SIG);
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

        // 3. resumable download of lib.zip
        ResumableDownloader.download(libZipUrl, tempDir.getPath(), LIB_ZIP, connectTimeout, readTimeout);

        // 4. clear any stale payload left by a previous (failed) attempt before unzipping into .TEMP/.
        //    Otherwise old jars not present in the new manifest survive and trip the allowlist
        //    (findUnexpectedFiles), making a valid update fail until .TEMP/ is cleaned by hand. The
        //    just-downloaded control files (manifest, signature, lib.zip) are preserved.
        clearStalePayload(tempDir);
        ZipExtractor.extract(new File(tempDir, LIB_ZIP), tempDir);

        // lib.zip may ship its own MANIFEST.MF; restore the signature-verified bytes so the manifest
        // that run.bat copies into lib/ is the trusted one, not the (unverified) archived copy.
        Files.write(manifestFile.toPath(), manifestBytes);

        // 5. verify each extracted file against the verified manifest, and reject any extra file the
        //    manifest does not list (allowlist).
        List<String> mismatched = ManifestVerifier.findMismatchedFiles(trustedManifest, tempDir);
        List<String> unexpected = ManifestVerifier.findUnexpectedFiles(trustedManifest, tempDir, CONTROL_FILES);
        if (!mismatched.isEmpty() || !unexpected.isEmpty()) {
            LOGGER.error("Extracted lib failed integrity check — mismatched: {}, unexpected: {}", mismatched, unexpected);
            return LibUpdateResult.VERIFY_FAILED;
        }

        LOGGER.info("lib update staged in {} — restart required to apply", tempDir);
        return LibUpdateResult.READY_RESTART;
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
     * Removes everything in {@code tempDir} except the just-downloaded control files
     * ({@code MANIFEST.MF}, {@code MANIFEST.MF.sig}, {@code lib.zip}), so a reused staging directory
     * cannot carry stale extracted jars into the next update's allowlist check. Does not follow symlinks.
     */
    private static void clearStalePayload(File tempDir) throws IOException {
        File[] entries = tempDir.listFiles();
        if (entries == null) {
            return;
        }
        for (File entry : entries) {
            if (CONTROL_FILES.contains(entry.getName())) {
                continue;
            }
            Files.walkFileTree(entry.toPath(), new SimpleFileVisitor<Path>() {
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
        }
    }
}

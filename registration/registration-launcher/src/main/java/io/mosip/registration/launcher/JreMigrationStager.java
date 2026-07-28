/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher;

import io.mosip.registration.launcher.common.DownloadProgressListener;
import io.mosip.registration.launcher.common.ManifestVerifier;
import io.mosip.registration.launcher.common.ZipExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.PublicKey;
import java.util.jar.Manifest;

import static io.mosip.registration.launcher.MigrationArtifacts.DIR_ARTIFACTS;
import static io.mosip.registration.launcher.MigrationArtifacts.DIR_JRE21_TEMP;
import static io.mosip.registration.launcher.MigrationArtifacts.DIR_LIB;
import static io.mosip.registration.launcher.MigrationArtifacts.DIR_TEMP;
import static io.mosip.registration.launcher.MigrationArtifacts.FILE_JRE21_ZIP;
import static io.mosip.registration.launcher.MigrationArtifacts.FILE_LAUNCHER;
import static io.mosip.registration.launcher.MigrationArtifacts.FILE_MIGRATION_EXE;
import static io.mosip.registration.launcher.MigrationArtifacts.FILE_ROLLBACK_EXE;
import static io.mosip.registration.launcher.MigrationArtifacts.FILE_RUN_BAT;
import static io.mosip.registration.launcher.MigrationArtifacts.FILE_RUN_BAT_BACKUP;

/**
 * Step-3 preparation for the JRE 11 -&gt; 21 migration path (design doc step 3): stages everything
 * {@code migration.exe} needs, <b>except the final "start migration.exe and exit" action</b>, which
 * is blocked on the native-exe toolchain decision (T3) and is performed by the caller.
 * <p>
 * All downloaded/staged content is integrity-verified before it is placed or made runnable: the lib
 * is staged via {@link LibUpdater} (signature + per-file hash + allowlist), and the migration
 * artifacts ({@code jre21.zip}, the exes, {@code _launcher.jar}) are checked against the
 * signature-verified root {@code MANIFEST.MF} before they are unzipped/copied.
 * <p>
 * Idempotent: safe to re-run after an interrupted attempt (each step is guarded by an existence check).
 */
public final class JreMigrationStager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JreMigrationStager.class);

    /** Artifacts copied from {@code lib/} to {@code .artifacts/} during the one-time {@code <1.3.0 -> 1.3.0} transition. */
    private static final String[] TRANSITION_ARTIFACTS = {
            FILE_LAUNCHER, FILE_JRE21_ZIP, FILE_MIGRATION_EXE, FILE_ROLLBACK_EXE, FILE_RUN_BAT
    };

    /** Migration artifacts that must be integrity-checked against the signed root manifest before use. */
    private static final String[] VERIFIED_ROOT_ARTIFACTS = {
            FILE_JRE21_ZIP, FILE_MIGRATION_EXE, FILE_ROLLBACK_EXE, FILE_LAUNCHER
    };

    private JreMigrationStager() {
        // utility class
    }

    /**
     * Prepares the JRE migration. After this returns, the caller should launch {@code migration.exe}
     * and exit the JVM.
     *
     * @param root                the application root
     * @param verifiedRootManifest the signature-verified root {@code MANIFEST.MF}, parsed from the
     *                            bytes whose {@code .sig} {@link StartupEvaluator} already checked —
     *                            passed in (not re-read from disk) so the integrity gate below cannot
     *                            be subverted by a {@code ./MANIFEST.MF} swapped after that check
     * @param libManifestUrl      URL of {@code lib/MANIFEST.MF} for the target version
     * @param libManifestSigUrl   URL of {@code lib/MANIFEST.MF.sig}
     * @param libZipUrl           URL of {@code lib.zip}
     * @param trustedKey          public key from the embedded {@code provider.pem}
     * @param connectTimeout      connection timeout (ms)
     * @param readTimeout         read timeout (ms)
     * @throws IOException       if a required artifact is missing or fails integrity verification
     * @throws SecurityException if the downloaded {@code lib/MANIFEST.MF.sig} is invalid (possible
     *                           tamper/MITM) — surfaced distinctly so the operator sees a security
     *                           alert rather than a generic failure
     */
    public static void stage(File root, Manifest verifiedRootManifest,
                             String libManifestUrl, String libManifestSigUrl, String libZipUrl,
                             PublicKey trustedKey, int connectTimeout, int readTimeout) throws IOException {
        stage(root, verifiedRootManifest, libManifestUrl, libManifestSigUrl, libZipUrl,
                trustedKey, connectTimeout, readTimeout, null);
    }

    /**
     * As {@link #stage(File, Manifest, String, String, String, PublicKey, int, int)}, additionally
     * reporting the {@code lib.zip} download's byte progress to {@code progress} (may be {@code null}).
     */
    public static void stage(File root, Manifest verifiedRootManifest,
                             String libManifestUrl, String libManifestSigUrl, String libZipUrl,
                             PublicKey trustedKey, int connectTimeout, int readTimeout,
                             DownloadProgressListener progress) throws IOException {
        File artifacts = new File(root, DIR_ARTIFACTS);
        File temp = new File(root, DIR_TEMP);
        File lib = new File(root, DIR_LIB);
        File jre21Temp = new File(root, DIR_JRE21_TEMP);
        Files.createDirectories(artifacts.toPath());

        // 1. <1.3.0 -> 1.3.0 transition: protect artifacts by copying lib/* -> .artifacts/* before lib cleanup.
        for (String name : TRANSITION_ARTIFACTS) {
            File inLib = new File(lib, name);
            File inArtifacts = new File(artifacts, name);
            if (inLib.exists() && !inArtifacts.exists()) {
                LOGGER.info("Transition: copying {} from lib/ to .artifacts/", name);
                copy(inLib, inArtifacts);
            }
        }

        // 2. integrity-check the migration artifacts against the signature-verified root manifest
        //    BEFORE any of them are unzipped (jre21.zip) or made runnable (exes / _launcher.jar).
        verifyArtifactsAgainstRootManifest(verifiedRootManifest, artifacts);

        // 3. stage the lib into .TEMP/ with full verification (signature + per-file hash + allowlist).
        LibUpdateResult libResult = LibUpdater.update(libManifestUrl, libManifestSigUrl, libZipUrl,
                temp, trustedKey, connectTimeout, readTimeout, progress);
        if (libResult == LibUpdateResult.ABORT_INVALID_SIGNATURE) {
            // Case B (tamper/MITM): preserve the security distinction across the stage() boundary so the
            // operator sees the same "signature invalid" alert as the lib-only update path, not a
            // generic "failed to prepare" message that invites a retry.
            throw new SecurityException("lib/MANIFEST.MF signature is invalid (possible tamper/MITM)");
        }
        if (libResult != LibUpdateResult.READY_RESTART) {
            throw new IOException("lib staging failed integrity verification: " + libResult);
        }

        // 4. unzip the verified jre21.zip -> jre21_temp/ (only if not already staged)
        if (!jre21Temp.exists()) {
            File jre21Zip = new File(artifacts, FILE_JRE21_ZIP);
            if (!jre21Zip.exists()) {
                throw new IOException("Missing " + jre21Zip.getPath() + " required for JRE migration");
            }
            ZipExtractor.extract(jre21Zip, jre21Temp);
        }

        // 5 & 6. copy the verified migration.exe / rollback.exe -> app root. Required now that the
        //    build pipeline produces them: fail closed if either is missing rather than leave the
        //    launcher with no binary to run. Idempotent when already staged by a prior run.
        copyRequired(new File(artifacts, FILE_MIGRATION_EXE), new File(root, FILE_MIGRATION_EXE));
        copyRequired(new File(artifacts, FILE_ROLLBACK_EXE), new File(root, FILE_ROLLBACK_EXE));

        // 7. backup run.bat -> run.bat_jre11 (once)
        File runBat = new File(root, FILE_RUN_BAT);
        File runBatBackup = new File(root, FILE_RUN_BAT_BACKUP);
        if (runBat.exists() && !runBatBackup.exists()) {
            LOGGER.info("Backing up run.bat -> run.bat_jre11");
            copy(runBat, runBatBackup);
        }

        LOGGER.info("JRE migration staged (verified); caller should now launch migration.exe and exit");
    }

    /**
     * Verifies each present migration artifact in {@code .artifacts/} against its hash in the
     * signature-verified root manifest. A mismatch — or a present artifact with no manifest entry to
     * verify it against — aborts the migration (fail closed): an unverifiable artifact must never be
     * unzipped/copied/made runnable.
     */
    private static void verifyArtifactsAgainstRootManifest(Manifest rootMf, File artifacts) throws IOException {
        for (String name : VERIFIED_ROOT_ARTIFACTS) {
            File artifact = new File(artifacts, name);
            if (!artifact.exists()) {
                // Absent artifacts are handled by later steps (e.g. jre21.zip throws if still missing).
                continue;
            }
            if (!ManifestVerifier.hasEntry(rootMf, name)) {
                // Fail closed: an artifact that is physically present but absent from the
                // signature-verified root manifest cannot be integrity-checked. Refuse to unzip/copy/
                // make it runnable rather than trusting unverifiable bytes — this matters most for the
                // T3 "launch migration.exe" step, which must never execute an unverified binary.
                // (Requires T5 to list jre21.zip/migration.exe/rollback.exe/_launcher.jar in the root
                // manifest; until then a missing entry correctly aborts the migration.)
                throw new IOException("Root manifest has no integrity entry for migration artifact: "
                        + name + " — refusing to use an unverifiable artifact");
            }
            if (!ManifestVerifier.fileMatches(rootMf, name, artifact)) {
                throw new IOException("Integrity check failed for migration artifact: " + name);
            }
            LOGGER.info("Verified migration artifact against root manifest: {}", name);
        }
    }

    private static void copyRequired(File src, File dst) throws IOException {
        if (!src.exists()) {
            // The exes are produced by the build pipeline (configure.sh) and staged into .artifacts/; a
            // missing one is a broken bundle -> fail closed rather than leave the launcher with no
            // migration.exe / rollback.exe to run.
            throw new IOException(src.getName() + " missing from .artifacts/ — required to run the JRE migration");
        }
        if (dst.exists()) {
            return; // idempotent: already staged by a prior (interrupted) run
        }
        copy(src, dst);
    }

    private static void copy(File src, File dst) throws IOException {
        File parent = dst.getAbsoluteFile().getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
        Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}

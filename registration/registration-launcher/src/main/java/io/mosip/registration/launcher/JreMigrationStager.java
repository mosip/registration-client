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
import static io.mosip.registration.launcher.MigrationArtifacts.DIR_JRE21_TEMP_PARTIAL;
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
 * artifacts ({@code jre21.zip}, the exes, {@code _launcher.jar}, {@code run.bat}) are checked against
 * the signature-verified root {@code MANIFEST.MF} before they are unzipped/copied.
 * <p>
 * Idempotent: safe to re-run after an interrupted attempt. Most steps are guarded by an existence
 * check; steps 5 and 6 instead re-copy the exes whenever the app-root copy differs from the
 * just-verified {@code .artifacts/} copy, so the binary that runs is always the binary that was
 * verified (see {@link #copyRequired}). This diverges from design step 3's literal "if not in app
 * root -> copy" wording, which assumes the app root can only ever hold THIS migration's exe.
 */
public final class JreMigrationStager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JreMigrationStager.class);

    /** Artifacts copied from {@code lib/} to {@code .artifacts/} during the one-time {@code <1.3.0 -> 1.3.0} transition. */
    private static final String[] TRANSITION_ARTIFACTS = {
            FILE_LAUNCHER, FILE_JRE21_ZIP, FILE_MIGRATION_EXE, FILE_ROLLBACK_EXE, FILE_RUN_BAT
    };

    /**
     * Artifacts {@code migration.exe} copies out of {@code .artifacts/} <b>after</b> it has emptied
     * {@code lib/} and renamed the new JRE over {@code jre/} — past the point where its own rollback
     * trigger is switched off. If either is missing there, the migration fails with the old lib gone,
     * the new JRE in place and no {@code _launcher.jar} to boot: an unbootable install that no retry
     * can repair. Presence is therefore required <i>before</i> the exe is ever started, which is the
     * only point at which failing is still safe (design constraint 5: a failed migration must remain
     * rollback-able). Note this cannot be left to
     * {@link #verifyArtifactsAgainstRootManifest} — that deliberately skips absent artifacts.
     */
    private static final String[] MIGRATION_INPUTS = {FILE_LAUNCHER, FILE_RUN_BAT};

    /**
     * Migration artifacts that must be integrity-checked against the signed root manifest before use.
     * <p>
     * {@code run.bat} is included because {@code migration.exe} copies {@code .artifacts/run.bat} into
     * the application root, where it becomes the script that launches the client — so it is as
     * execution-sensitive as the exes, and the design's Case-A scenario (N3) expects its hash to be
     * checked. The launcher only detects a mismatch (fail closed); the AC11 re-download recovery for
     * root artifacts is not yet implemented.
     */
    private static final String[] VERIFIED_ROOT_ARTIFACTS = {
            FILE_JRE21_ZIP, FILE_MIGRATION_EXE, FILE_ROLLBACK_EXE, FILE_LAUNCHER, FILE_RUN_BAT
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

        // 4. unzip the verified jre21.zip -> jre21_temp/ (only if not already staged).
        //    Extraction goes into jre21_temp.partial/ and is renamed to jre21_temp/ only on success, so
        //    a crash mid-unzip can never leave a half-written tree named jre21_temp/. Testing
        //    jre21Temp.exists() on its own treated such a tree as staged: the next start skipped step 4
        //    and reported success, and migration.exe promoted a broken JRE over the working one -- an
        //    unbootable client. The design requires exactly this split: a partial tree is discarded and
        //    the unzip re-attempted, while a COMPLETE one is skipped so a re-invoked migration.exe stays
        //    idempotent instead of re-extracting ~200MB. migration.exe uses the same
        //    jre21_temp.partial/ protocol, so a tree left behind by either component is discarded and
        //    re-extracted by the other.
        //    NOTE: this makes jre21_temp/ trustworthy against interruption, NOT against tampering -- it
        //    is the one migration input with no hash in the root manifest. Anything able to plant a
        //    directory in the app root can equally overwrite lib/_launcher.jar, so this adds no exposure
        //    the installation does not already have.
        //
        //    The stale-partial cleanup sits OUTSIDE the skip: a run that was interrupted mid-unzip and
        //    then completed on a later attempt leaves jre21_temp.partial/ behind forever otherwise --
        //    rollback.exe removes only jre21_temp/, and MigrationCleaner runs solely on normal startup,
        //    which is unreachable while the version mismatch driving this migration persists.
        File partial = new File(root, DIR_JRE21_TEMP_PARTIAL);
        if (partial.exists() && !MigrationCleaner.deleteRecursively(partial)) {
            throw new IOException("Could not clear stale " + partial.getPath()
                    + " left by an interrupted extraction");
        }
        if (!jre21Temp.exists()) {
            File jre21Zip = new File(artifacts, FILE_JRE21_ZIP);
            if (!jre21Zip.exists()) {
                throw new IOException("Missing " + jre21Zip.getPath() + " required for JRE migration");
            }
            ZipExtractor.extract(jre21Zip, partial);
            try {
                // Files.move, not File.renameTo: renameTo returns a bare false that discards the OS
                // error, and on Windows a transient handle on any one of the thousands of just-written
                // JRE files (indexer, AV scan) is enough to fail it. An exception carries the real cause
                // into the log instead of an unexplained abort, and copyRequired below already reasons
                // about this same Windows hazard.
                Files.move(partial.toPath(), jre21Temp.toPath());
            } catch (IOException e) {
                // Leave the extracted tree under .partial/: the next attempt clears and re-extracts it.
                // Promoting it by any other route would defeat the completeness guarantee.
                throw new IOException("Could not finalize the extracted JRE: " + partial.getPath()
                        + " -> " + jre21Temp.getPath(), e);
            }
        }

        // 5 & 6. copy the verified migration.exe / rollback.exe -> app root. Required now that the
        //    build pipeline produces them: fail closed if either is missing rather than leave the
        //    launcher with no binary to run. Overwrites any app-root copy from a prior attempt so the
        //    binary that runs is always the one verified above.
        copyRequired(verifiedRootManifest, FILE_MIGRATION_EXE,
                new File(artifacts, FILE_MIGRATION_EXE), new File(root, FILE_MIGRATION_EXE));
        copyRequired(verifiedRootManifest, FILE_ROLLBACK_EXE,
                new File(artifacts, FILE_ROLLBACK_EXE), new File(root, FILE_ROLLBACK_EXE));

        // 7. backup run.bat -> run.bat_jre11 (once)
        File runBat = new File(root, FILE_RUN_BAT);
        File runBatBackup = new File(root, FILE_RUN_BAT_BACKUP);
        if (runBat.exists() && !runBatBackup.exists()) {
            LOGGER.info("Backing up run.bat -> run.bat_jre11");
            copy(runBat, runBatBackup);
        }

        // 8. last gate before the point of no return: everything migration.exe will consume from
        //    .artifacts/ after the swap must actually be there. Verified already (step 2) if present;
        //    this closes the "absent artifacts are skipped" hole in that check.
        requireMigrationInputs(artifacts);

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

    /**
     * Fails closed unless every {@link #MIGRATION_INPUTS} entry is staged in {@code .artifacts/}.
     */
    private static void requireMigrationInputs(File artifacts) throws IOException {
        for (String name : MIGRATION_INPUTS) {
            File staged = new File(artifacts, name);
            if (!staged.exists()) {
                throw new IOException(name + " missing from .artifacts/ — migration.exe installs it after "
                        + "the JRE swap, where rollback is no longer possible; refusing to start the swap");
            }
        }
    }

    private static void copyRequired(Manifest rootMf, String name, File src, File dst) throws IOException {
        if (!src.exists()) {
            // The exes are produced by the build pipeline (configure.sh) and staged into .artifacts/; a
            // missing one is a broken bundle -> fail closed rather than leave the launcher with no
            // migration.exe / rollback.exe to run.
            throw new IOException(src.getName() + " missing from .artifacts/ — required to run the JRE migration");
        }
        // Re-copy only when the app-root copy does not already match the SIGNED root manifest. Checking
        // dst against the manifest (rather than hashing .artifacts/ a second time) costs one hash instead
        // of two and asserts the property that actually matters -- the binary that runs is the one the
        // manifest approves -- without depending on an earlier, unrelated method having verified src.
        // A root copy left behind by an earlier attempt can be a DIFFERENT version (or one never checked
        // against THIS manifest), and it is the copy MigrationLauncher executes — so the verified binary
        // and the running binary must never be allowed to diverge. Comparing first, rather than always
        // overwriting, keeps the re-run safe on Windows: a still-running (or AV-held) exe locks its own
        // image, and an unconditional REPLACE_EXISTING copy would throw AccessDeniedException and turn a
        // resumable retry into a permanent abort.
        if (dst.exists() && ManifestVerifier.fileMatches(rootMf, name, dst)) {
            return;
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

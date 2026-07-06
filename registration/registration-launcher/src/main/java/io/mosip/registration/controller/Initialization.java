/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.controller;

import io.mosip.registration.launcher.JreMigrationStager;
import io.mosip.registration.launcher.JreVersionDetector;
import io.mosip.registration.launcher.LauncherConfig;
import io.mosip.registration.launcher.LauncherDialogs;
import io.mosip.registration.launcher.LibUpdateResult;
import io.mosip.registration.launcher.LibUpdater;
import io.mosip.registration.launcher.MigrationCleaner;
import io.mosip.registration.launcher.NormalStartup;
import io.mosip.registration.launcher.StartupAction;
import io.mosip.registration.launcher.StartupEvaluator;
import io.mosip.registration.launcher.common.ManifestVerifier;
import io.mosip.registration.launcher.common.ResumableDownloader;
import io.mosip.registration.launcher.common.SignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.util.jar.Manifest;

/**
 * Entry point of {@code _launcher.jar} — the sole entry point of the Registration Client from 1.3.0
 * onwards (the {@code main()} in {@code registration-client.jar} is removed in T6).
 * <p>
 * Kept under the {@code io.mosip.registration.controller} package with the same FQN as the legacy
 * client entry point so the existing {@code run.bat} resolves it from {@code _launcher.jar} during
 * the {@code <1.3.0 -> 1.3.0} transition (classpath order: {@code _launcher.jar} sorts first).
 * <p>
 * <b>Constraint:</b> the migration code paths run under the Java 11 JRE and MUST NOT load any class
 * from {@code registration-client.jar} or other Java 21 jars. Normal startup (the Java 21 path) must
 * invoke {@code ClientApplication} reflectively, never via a compile-time import.
 */
public class Initialization {

    private static final Logger LOGGER = LoggerFactory.getLogger(Initialization.class);

    private static final File ROOT_MANIFEST = new File("MANIFEST.MF");
    private static final File ROOT_SIGNATURE = new File("MANIFEST.MF.sig");
    private static final File LIB_MANIFEST = new File("lib/MANIFEST.MF");
    private static final File TEMP_DIR = new File(".TEMP");
    private static final File APP_ROOT = new File(".");
    private static final File CONFIG_FILE = new File("mosip-application.properties");
    private static final String TRUSTED_CERT = "provider.pem";

    // 50s timeout for establishing the connection to the upgrade server.
    private static final int CONNECT_TIMEOUT = 50000;
    // 30s inactivity-between-bytes timeout (not a total cap); avoids hanging forever on a stalled connection.
    private static final int READ_TIMEOUT = 30000;
    // A detached SHA256withRSA signature is small (RSA-4096 -> 512 bytes); an empty file or anything
    // larger (e.g. an HTML error page) is a malformed signature — a Case C integrity failure.
    private static final long MAX_SIGNATURE_BYTES = 1024L;

    public static void main(String[] args) {
        try {
            // Detect the JRE inside the try: majorVersion() throws IllegalArgumentException for a
            // null/blank/unparseable java.version, and that must surface through the operator dialog +
            // controlled exit below rather than escaping main() as a raw stack trace.
            int jreMajor = JreVersionDetector.currentMajorVersion();
            LOGGER.info("_launcher.jar starting on JRE major version {}", jreMajor);

            PublicKey trustedKey = loadTrustedKey();
            StartupEvaluator.Evaluation evaluation = StartupEvaluator.evaluate(
                    ROOT_MANIFEST, ROOT_SIGNATURE, LIB_MANIFEST, trustedKey, jreMajor);
            handle(evaluation, jreMajor, args, trustedKey);
        } catch (Exception e) {
            LOGGER.error("Launcher startup failed", e);
            LauncherDialogs.error("Startup failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void handle(StartupEvaluator.Evaluation evaluation, int jreMajor, String[] args,
                               PublicKey trustedKey) {
        switch (evaluation.action()) {
            case SIGNATURE_MISSING:
                handleSignatureMissing(jreMajor, args, trustedKey);
                break;
            case ABORT_INVALID_SIGNATURE:
                // Case B: do not re-download anything (possible MITM); inform operator and exit.
                LauncherDialogs.error("Security alert: MANIFEST.MF signature invalid. Startup aborted.");
                System.exit(1);
                break;
            case NORMAL_STARTUP:
                LOGGER.info("Versions match — cleaning up migration artifacts and starting normally (step 6)");
                MigrationCleaner.cleanup(APP_ROOT);
                try {
                    NormalStartup.launch(args);
                } catch (ReflectiveOperationException e) {
                    LOGGER.error("Failed to launch ClientApplication", e);
                    LauncherDialogs.error("Failed to start the application: " + e.getMessage());
                    System.exit(1);
                }
                break;
            case MIGRATE_JRE:
                handleJreMigration(evaluation.verifiedRootManifest(), trustedKey);
                break;
            case UPDATE_LIB:
                handleLibUpdate(evaluation.verifiedRootManifest(), jreMajor, trustedKey);
                break;
            case ABORT_UNSUPPORTED_JRE:
                LOGGER.error("Unsupported JRE major version {} — cannot upgrade", jreMajor);
                LauncherDialogs.error("Unsupported Java runtime (version " + jreMajor
                        + "). Please reinstall the Registration Client.");
                System.exit(1);
                break;
            case ABORT_CORRUPT_LIB_MANIFEST:
                // lib/MANIFEST.MF present but unreadable — refuse to silently migrate; ask for repair.
                LOGGER.error("lib/MANIFEST.MF is corrupt — aborting startup");
                LauncherDialogs.error("The client installation appears corrupt (lib/MANIFEST.MF). "
                        + "Please repair or reinstall the Registration Client.");
                System.exit(1);
                break;
            case ABORT_CORRUPT_ROOT_MANIFEST:
                // Signature-valid root MANIFEST.MF but no version — corrupt/mispackaged; do not proceed.
                LOGGER.error("./MANIFEST.MF is corrupt (no Manifest-Version) — aborting startup");
                LauncherDialogs.error("The update manifest appears corrupt (MANIFEST.MF). "
                        + "Please repair or reinstall the Registration Client.");
                System.exit(1);
                break;
            default:
                // Fail closed: a StartupAction added later without a case here must NOT silently fall
                // through to a normal exit(0) — abort visibly so the gap is caught, not shipped.
                LOGGER.error("Unhandled startup action: {} — aborting", evaluation.action());
                LauncherDialogs.error("Startup failed: unrecognized startup state (" + evaluation.action() + ").");
                System.exit(1);
                break;
        }
    }

    /** Case C: download the missing root {@code MANIFEST.MF.sig}, then re-evaluate once. */
    private static void handleSignatureMissing(int jreMajor, String[] args, PublicKey trustedKey) {
        try {
            LauncherConfig config = LauncherConfig.load(CONFIG_FILE);
            // No verified manifest exists yet (the .sig is what's missing). Read the version from the
            // on-disk manifest only to build the download URL; if that manifest was tampered, the .sig
            // fetched for it will not verify on the re-evaluation below, so this read is safe.
            String version = requireVersion(ManifestVerifier.getVersion(ROOT_MANIFEST));
            LOGGER.info("Downloading missing MANIFEST.MF.sig for version {}", version);
            ResumableDownloader.download(config.rootManifestSigUrl(version),
                    APP_ROOT.getPath(), ROOT_SIGNATURE.getName(), CONNECT_TIMEOUT, READ_TIMEOUT);

            // Case C: a downloaded MANIFEST.MF.sig that fails its integrity check continues as Case B
            // (assume tamper/MITM; do not re-download). An empty body or an oversized one (e.g. an HTML
            // error page) is a clear integrity failure we can detect up front, so route it to the same
            // Case B security alert as the verification below rather than a softer message.
            long sigLength = ROOT_SIGNATURE.length();
            if (sigLength == 0L || sigLength > MAX_SIGNATURE_BYTES) {
                LOGGER.error("Downloaded MANIFEST.MF.sig is malformed (size {} bytes) — Case C integrity "
                        + "failure, aborting as Case B", sigLength);
                LauncherDialogs.error("Security alert: MANIFEST.MF signature invalid. Startup aborted.");
                System.exit(1);
                return;
            }

            StartupEvaluator.Evaluation evaluation = StartupEvaluator.evaluate(
                    ROOT_MANIFEST, ROOT_SIGNATURE, LIB_MANIFEST, trustedKey, jreMajor);
            if (evaluation.action() == StartupAction.SIGNATURE_MISSING) {
                LauncherDialogs.error("Unable to obtain MANIFEST.MF signature. Startup aborted.");
                System.exit(1);
            } else {
                handle(evaluation, jreMajor, args, trustedKey);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to download/verify MANIFEST.MF.sig", e);
            LauncherDialogs.error("Failed to obtain MANIFEST.MF signature: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Step 3: prepare the JRE migration, then launch {@code migration.exe} (launch blocked on T3). */
    private static void handleJreMigration(Manifest verifiedRootManifest, PublicKey trustedKey) {
        LOGGER.info("Version change on JRE 11 — preparing JRE migration (step 3)");
        try {
            LauncherConfig config = LauncherConfig.load(CONFIG_FILE);
            String version = requireVersion(ManifestVerifier.getVersion(verifiedRootManifest));
            JreMigrationStager.stage(APP_ROOT, verifiedRootManifest,
                    config.libManifestUrl(version), config.libManifestSigUrl(version), config.libZipUrl(version),
                    trustedKey, CONNECT_TIMEOUT, READ_TIMEOUT);
            // TODO(T3): replace the dialog+exit below with "launch migration.exe (native; toolchain
            //   reopened) and System.exit(0)" — migration.exe performs the JRE swap and exits.
            // Until then make the terminal state explicit (visible dialog + deliberate exit) so the
            // staged-but-not-executed migration is never an invisible fall-through to exit(0).
            LOGGER.warn("JRE migration staged; launching migration.exe is blocked on T3 (native-exe toolchain TBD)");
            LauncherDialogs.info("A required runtime update has been prepared. "
                    + "Completing it needs a newer client version — please contact your administrator.");
            System.exit(0);
        } catch (SecurityException e) {
            // Case B propagated from stage(): show the security alert, consistent with handleLibUpdate,
            // rather than the generic "failed to prepare" message that would invite a retry of a tamper.
            LOGGER.error("Security alert preparing JRE migration", e);
            LauncherDialogs.error("Security alert: lib signature invalid. Update aborted.");
            System.exit(1);
        } catch (Exception e) {
            LOGGER.error("Failed to prepare JRE migration", e);
            LauncherDialogs.error("Failed to prepare the update: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Step 5: download + verify + stage the new lib into {@code .TEMP/}, then prompt for restart. */
    private static void handleLibUpdate(Manifest verifiedRootManifest, int jreMajor, PublicKey trustedKey) {
        LOGGER.info("Version change on JRE {} — lib-only update path (step 5)", jreMajor);
        try {
            LauncherConfig config = LauncherConfig.load(CONFIG_FILE);
            String version = requireVersion(ManifestVerifier.getVersion(verifiedRootManifest));
            LibUpdateResult result = LibUpdater.update(
                    config.libManifestUrl(version), config.libManifestSigUrl(version), config.libZipUrl(version),
                    TEMP_DIR, trustedKey, CONNECT_TIMEOUT, READ_TIMEOUT);
            switch (result) {
                case READY_RESTART:
                    LauncherDialogs.info("Update ready. Please restart the application.");
                    System.exit(0);
                    break;
                case ABORT_INVALID_SIGNATURE:
                    LauncherDialogs.error("Security alert: lib signature invalid. Update aborted.");
                    System.exit(1);
                    break;
                case VERIFY_FAILED:
                    LauncherDialogs.error("Update integrity check failed. Please retry.");
                    System.exit(1);
                    break;
                default:
                    // Fail closed: a new LibUpdateResult without a case here must not fall through to a
                    // silent normal exit — abort visibly.
                    LOGGER.error("Unhandled lib update result: {} — aborting", result);
                    LauncherDialogs.error("Update failed: unrecognized result (" + result + ").");
                    System.exit(1);
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Lib update failed", e);
            LauncherDialogs.error("Update failed: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Validates a resolved manifest version, failing clearly if it is absent/blank. */
    private static String requireVersion(String version) throws IOException {
        if (version == null || version.trim().isEmpty()) {
            throw new IOException(ROOT_MANIFEST.getName() + " is missing the Manifest-Version attribute");
        }
        return version;
    }

    private static PublicKey loadTrustedKey() throws Exception {
        try (InputStream cert = Initialization.class.getClassLoader().getResourceAsStream(TRUSTED_CERT)) {
            if (cert == null) {
                throw new IllegalStateException(TRUSTED_CERT + " not found on the classpath");
            }
            return SignatureVerifier.loadPublicKeyFromCertificate(cert);
        }
    }
}

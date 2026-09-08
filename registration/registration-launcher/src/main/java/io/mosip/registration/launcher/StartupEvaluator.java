/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher;

import io.mosip.registration.launcher.common.ManifestVerifier;
import io.mosip.registration.launcher.common.SignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.PublicKey;
import java.util.jar.Manifest;

/**
 * Step 2 of the launcher (design doc): on every startup, verify the detached signature of the root
 * {@code ./MANIFEST.MF} and compare its version against {@code lib/MANIFEST.MF}, then decide what to
 * do next. This class holds only the <b>pure decision</b> — downloading a missing signature, showing
 * dialogs, exiting the JVM and the actual migrate/update/startup work are side effects handled by the
 * {@code Initialization} entry point based on the returned {@link StartupAction}.
 */
public final class StartupEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupEvaluator.class);
    private static final int JAVA_11 = 11;
    private static final int JAVA_21 = 21;

    private StartupEvaluator() {
        // utility class
    }

    /**
     * Evaluates the startup state.
     *
     * @param rootManifest     the orchestration manifest {@code ./MANIFEST.MF}
     * @param rootSignature    its detached signature {@code ./MANIFEST.MF.sig} (may be absent)
     * @param libManifest      the per-file manifest {@code lib/MANIFEST.MF} (may be absent pre-1.3.0)
     * @param trustedKey       the public key from the embedded {@code provider.pem}
     * @param jreMajorVersion  the running JRE major version (see {@link JreVersionDetector})
     * @return the action the entry point must take next, together with the signature-verified root
     *         manifest (when one was verified) so callers reuse it instead of re-reading the file
     * @throws IOException if {@code rootManifest} cannot be read
     */
    public static Evaluation evaluate(File rootManifest, File rootSignature, File libManifest,
                                      PublicKey trustedKey, int jreMajorVersion) throws IOException {
        // --- signature gate (Cases B / C) ---
        if (rootSignature == null || !rootSignature.exists()) {
            LOGGER.info("Root MANIFEST.MF.sig not present");
            return new Evaluation(StartupAction.SIGNATURE_MISSING, null);
        }
        byte[] manifestBytes = Files.readAllBytes(rootManifest.toPath());
        byte[] signatureBytes = Files.readAllBytes(rootSignature.toPath());
        if (!SignatureVerifier.verify(manifestBytes, signatureBytes, trustedKey)) {
            LOGGER.error("Root MANIFEST.MF signature is INVALID — aborting startup (possible tamper/MITM)");
            return new Evaluation(StartupAction.ABORT_INVALID_SIGNATURE, null);
        }

        // Parse the signature-verified bytes ONCE; the verified manifest is carried out on the
        // Evaluation so migrate/update handlers act on these exact bytes rather than re-reading (and
        // re-trusting) ./MANIFEST.MF from disk after the signature check.
        Manifest verifiedManifest = ManifestVerifier.parse(manifestBytes);

        // A signature-valid root manifest with no Manifest-Version is corrupt/mispackaged: the
        // orchestration version drives every decision below, so abort rather than fall through to a
        // migration/update path (mirrors the corrupt-lib-manifest handling).
        String rootVersion = ManifestVerifier.getVersion(verifiedManifest);
        if (rootVersion == null || rootVersion.trim().isEmpty()) {
            LOGGER.error("Root MANIFEST.MF is signature-valid but has no Manifest-Version (corrupt) — aborting");
            return new Evaluation(StartupAction.ABORT_CORRUPT_ROOT_MANIFEST, verifiedManifest);
        }

        // --- version comparison against lib/MANIFEST.MF ---
        // An ABSENT lib manifest (pre-1.3.0 / fresh install) legitimately means "migration required".
        if (libManifest == null || !libManifest.exists()) {
            LOGGER.info("No lib/MANIFEST.MF present (pre-1.3.0) — migration required (JRE major version {})",
                    jreMajorVersion);
            return new Evaluation(migrationAction(jreMajorVersion), verifiedManifest);
        }

        String libVersion = ManifestVerifier.getVersion(libManifest);

        // A PRESENT-but-unreadable lib manifest is corruption, not a version change: do NOT silently run
        // a heavy JRE migration on a possibly-current machine — abort and let the operator repair.
        if (libVersion == null || libVersion.trim().isEmpty()) {
            LOGGER.error("lib/MANIFEST.MF is present but has no Manifest-Version (corrupt/truncated) — aborting");
            return new Evaluation(StartupAction.ABORT_CORRUPT_LIB_MANIFEST, verifiedManifest);
        }
        if (rootVersion.equals(libVersion)) {
            LOGGER.info("Root and lib manifest versions match — normal startup");
            return new Evaluation(StartupAction.NORMAL_STARTUP, verifiedManifest);
        }

        LOGGER.info("Manifest versions differ (root={}, lib={}) — migration required (JRE major version {})",
                rootVersion, libVersion, jreMajorVersion);
        return new Evaluation(migrationAction(jreMajorVersion), verifiedManifest);
    }

    /**
     * The outcome of {@link #evaluate}: the action to take plus, when the root signature verified, the
     * parsed signature-verified root manifest. {@link #verifiedRootManifest()} is {@code null} for
     * {@link StartupAction#SIGNATURE_MISSING} and {@link StartupAction#ABORT_INVALID_SIGNATURE}, where
     * no trusted manifest exists.
     */
    public static final class Evaluation {
        private final StartupAction action;
        private final Manifest verifiedRootManifest;

        Evaluation(StartupAction action, Manifest verifiedRootManifest) {
            this.action = action;
            this.verifiedRootManifest = verifiedRootManifest;
        }

        public StartupAction action() {
            return action;
        }

        public Manifest verifiedRootManifest() {
            return verifiedRootManifest;
        }
    }

    /** Maps a confirmed version difference to the migration path for the running JRE. */
    private static StartupAction migrationAction(int jreMajorVersion) {
        if (jreMajorVersion == JAVA_11) {
            return StartupAction.MIGRATE_JRE;
        }
        if (jreMajorVersion >= JAVA_21) {
            return StartupAction.UPDATE_LIB;
        }
        LOGGER.error("Unsupported JRE major version {} for upgrade (expected 11 or >= 21)", jreMajorVersion);
        return StartupAction.ABORT_UNSUPPORTED_JRE;
    }
}

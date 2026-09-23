/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher;

/**
 * The outcome of the launcher's step-2 startup evaluation (design doc step 2 + signature Cases B/C).
 * Each value maps to the next action the {@code Initialization} entry point must take.
 */
public enum StartupAction {

    /** {@code ./MANIFEST.MF.sig} is absent — download it from the upgrade server, then re-evaluate (Case C). */
    SIGNATURE_MISSING,

    /** {@code ./MANIFEST.MF.sig} is present but invalid — show error and exit, re-download nothing (Case B, possible MITM). */
    ABORT_INVALID_SIGNATURE,

    /** Root and lib manifest versions match and {@code lib/MANIFEST.MF} is signature-valid — proceed to normal startup (step 6). */
    NORMAL_STARTUP,

    /**
     * Versions match, but {@code lib/MANIFEST.MF.sig} is absent — download it from the upgrade server
     * and re-evaluate (Case C for the lib manifest). Only reachable on the versions-match branch, where
     * {@code lib/MANIFEST.MF} is about to be trusted; a 1.3.0+ bundle always ships the signature, so an
     * absent one means it was removed.
     */
    LIB_SIGNATURE_MISSING,

    /**
     * {@code lib/MANIFEST.MF} is present but its signature does not verify — show error and exit,
     * re-download nothing (Case B for the lib manifest, possible tamper). Without this gate a jar and
     * its {@code lib/MANIFEST.MF} hash entry rewritten together are mutually consistent, so the
     * per-file check alone reports the pair as valid on every later launch.
     * <p>
     * Scope: this catches tampering that is <b>on disk when the launcher runs</b>. It does not close the
     * check-then-use window — the verified bytes are not carried into the client JVM, where
     * {@code ClientSetupValidator} / {@code ClientIntegrityValidator} re-read {@code lib/MANIFEST.MF}
     * from disk without a signature check — so an attacker able to write {@code lib/} during the
     * seconds of JVM boot is still unhandled. Closing that needs the client side to verify too.
     */
    ABORT_INVALID_LIB_SIGNATURE,

    /** Versions differ and the current JRE is 11 — take the JRE migration path (step 3, launches migration.exe). */
    MIGRATE_JRE,

    /** Versions differ and the current JRE is 21+ — take the lib-only update path (step 5). */
    UPDATE_LIB,

    /** Versions differ but the current JRE is neither 11 nor 21+ (e.g. a stale Java 8) — cannot safely proceed. */
    ABORT_UNSUPPORTED_JRE,

    /**
     * {@code lib/MANIFEST.MF} is present but carries no readable {@code Manifest-Version} (corrupt or
     * truncated). This is distinct from a legitimately absent lib manifest (pre-1.3.0 → migrate): an
     * unreadable-but-present manifest must NOT silently trigger a heavy JRE migration on a possibly
     * current machine, so startup aborts and asks the operator to repair/reinstall.
     */
    ABORT_CORRUPT_LIB_MANIFEST,

    /**
     * The root {@code ./MANIFEST.MF} itself is absent. Distinct from {@link #SIGNATURE_MISSING}: there
     * is no manifest to fetch a signature for, so this is a broken installation needing repair rather
     * than a Case C signature download.
     */
    ABORT_MISSING_ROOT_MANIFEST,

    /**
     * The root {@code ./MANIFEST.MF} is signature-valid but carries no readable {@code Manifest-Version}
     * (corrupt or mispackaged). The orchestration version drives every upgrade decision, so a missing
     * version must be a hard stop rather than silently falling through to a migration/update path.
     */
    ABORT_CORRUPT_ROOT_MANIFEST
}

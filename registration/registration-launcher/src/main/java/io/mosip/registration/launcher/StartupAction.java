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

    /** Root and lib manifest versions match — nothing to migrate; proceed to normal startup (step 6). */
    NORMAL_STARTUP,

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
     * The root {@code ./MANIFEST.MF} is signature-valid but carries no readable {@code Manifest-Version}
     * (corrupt or mispackaged). The orchestration version drives every upgrade decision, so a missing
     * version must be a hard stop rather than silently falling through to a migration/update path.
     */
    ABORT_CORRUPT_ROOT_MANIFEST
}

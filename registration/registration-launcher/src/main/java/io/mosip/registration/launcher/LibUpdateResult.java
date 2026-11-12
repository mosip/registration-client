/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher;

/**
 * Outcome of the step-5 (JRE 21+) lib-only update performed by {@link LibUpdater}.
 */
public enum LibUpdateResult {

    /** New lib staged in {@code .TEMP/} and integrity-verified; operator must restart. */
    READY_RESTART,

    /** {@code lib/MANIFEST.MF.sig} did not verify — abort, do not apply (Case B). */
    ABORT_INVALID_SIGNATURE,

    /** A file extracted from {@code lib.zip} did not match its hash in {@code lib/MANIFEST.MF}. */
    VERIFY_FAILED
}

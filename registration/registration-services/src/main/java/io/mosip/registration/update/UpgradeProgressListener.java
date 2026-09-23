/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.update;

/**
 * Reports how far an in-app software upgrade download has progressed, so the UI can show real
 * progress while the operator keeps working.
 * <p>
 * Callbacks arrive on the download thread, never on the JavaFX application thread — implementations
 * must not touch the scene graph directly. {@code Task.updateProgress} is safe to call from here.
 */
@FunctionalInterface
public interface UpgradeProgressListener {

    /** A listener that discards every update, for callers that do not want progress. */
    UpgradeProgressListener NO_OP = (fraction, artifact) -> {
        // no progress reporting requested
    };

    /**
     * @param fraction overall progress across every artifact, in {@code [0.0, 1.0]}, or a negative
     *                 value when the total is not yet known (the UI should stay indeterminate)
     * @param artifact name of the artifact currently being fetched, for display
     */
    void onProgress(double fraction, String artifact);
}

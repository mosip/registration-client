/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher.common;

/**
 * Receives an operator-facing status line from a long-running upgrade step, so the step can tell the
 * operator what it is doing without knowing how (or whether) that is displayed.
 * <p>
 * Kept in {@code launcher.common} with no Swing dependency, for the same reason as
 * {@link DownloadProgressListener}: the staging logic must stay UI-free and headless-safe, while the
 * launcher wires this to the live progress window. The design requires such an alert before a failed
 * artifact is re-downloaded ("<i>&lt;filename&gt; integrity check failed. Restoring from server...</i>").
 */
@FunctionalInterface
public interface OperatorAlertListener {

    /**
     * @param message the operator-facing line; implementations must not block the calling thread, which
     *                is the thread performing the upgrade
     */
    void onAlert(String message);
}

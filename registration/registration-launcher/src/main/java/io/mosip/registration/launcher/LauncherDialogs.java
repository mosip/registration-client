/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import java.awt.GraphicsEnvironment;

/**
 * Operator-facing dialogs for the launcher. Every message is also logged, and dialogs are skipped
 * in headless environments (design T2: {@code GraphicsEnvironment.isHeadless()} guard) so the
 * launcher can run unattended / in CI without blocking.
 */
public final class LauncherDialogs {

    private static final Logger LOGGER = LoggerFactory.getLogger(LauncherDialogs.class);
    private static final String TITLE = "Registration Client";

    private LauncherDialogs() {
        // utility class
    }

    public static void error(String message) {
        LOGGER.error(message);
        show(message, JOptionPane.ERROR_MESSAGE);
    }

    public static void info(String message) {
        LOGGER.info(message);
        show(message, JOptionPane.INFORMATION_MESSAGE);
    }

    private static void show(String message, int messageType) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        JOptionPane.showMessageDialog(null, message, TITLE, messageType);
    }
}

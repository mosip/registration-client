/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;

/**
 * Operator-facing dialogs for the launcher. Every message is also logged, and dialogs are skipped
 * in headless environments (design T2: {@code GraphicsEnvironment.isHeadless()} guard) so the
 * launcher can run unattended / in CI without blocking.
 */
public final class LauncherDialogs {

    private static final Logger LOGGER = LoggerFactory.getLogger(LauncherDialogs.class);
    private static final String TITLE = "Registration Client";

    /**
     * A modeless progress window, dismissed via {@link #close()}. Implements {@link AutoCloseable} so
     * callers may use try-with-resources, but {@link #close()} never throws and is safe to call from
     * any thread and more than once — a progress UI must never break the upgrade it is reporting on.
     */
    public interface ProgressHandle extends AutoCloseable {
        @Override
        void close();

        /**
         * Advances the bar toward {@code bytesDone / total} (a percentage), flipping it from the initial
         * indeterminate spinner to a determinate value on the first call. A no-op on the headless /
         * no-op handle and while {@code total <= 0} (size unknown — stays indeterminate). Safe to call
         * from any thread.
         */
        default void update(long bytesDone, long total) {
            // no-op unless a real window is showing (overridden by the Swing handle)
        }
    }

    /** Returned on the headless path and whenever the window cannot be built — the upgrade proceeds silently. */
    private static final ProgressHandle NO_OP = () -> { };

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

    /**
     * Shows a modeless, indeterminate progress window for a long-running step (download / verify /
     * unzip during staging) and returns immediately. The window is built and shown on the EDT; the
     * caller's slow work then runs on its own thread while the bar animates independently. In a
     * headless JVM — or if the window cannot be built for any reason — a no-op handle is returned so
     * the upgrade proceeds without a UI rather than failing.
     *
     * @param message the operator-facing status line (also logged)
     * @return a handle whose {@link ProgressHandle#close()} disposes the window
     */
    public static ProgressHandle progress(String message) {
        LOGGER.info(message);
        if (GraphicsEnvironment.isHeadless()) {
            return NO_OP;
        }
        try {
            final SwingProgressHandle[] holder = new SwingProgressHandle[1];
            SwingUtilities.invokeAndWait(() -> holder[0] = buildProgressDialog(message));
            return holder[0];
        } catch (Exception e) {
            // A progress window must never break the upgrade: fall back to running without one.
            LOGGER.warn("Could not display the progress window ({}); continuing without it", e.getMessage());
            return NO_OP;
        }
    }

    private static void show(String message, int messageType) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        JOptionPane.showMessageDialog(null, message, TITLE, messageType);
    }

    /** Builds and shows the modeless progress window. Must be invoked on the EDT. */
    private static SwingProgressHandle buildProgressDialog(String message) {
        JDialog dialog = new JDialog((Frame) null, TITLE, false); // modeless -> setVisible returns at once
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); // operator can't cancel a running upgrade
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setIndeterminate(true); // a spinner until the first byte-progress arrives (or if the size is unknown)
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        panel.add(new JLabel(message), BorderLayout.NORTH);
        panel.add(bar, BorderLayout.CENTER);
        dialog.setContentPane(panel);
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        return new SwingProgressHandle(dialog, bar);
    }

    /** The concrete progress window: disposes on close, and flips from spinner to a percentage on update. */
    private static final class SwingProgressHandle implements ProgressHandle {
        private final JDialog dialog;
        private final JProgressBar bar;

        SwingProgressHandle(JDialog dialog, JProgressBar bar) {
            this.dialog = dialog;
            this.bar = bar;
        }

        @Override
        public void close() {
            // dispose() is idempotent; invokeLater is safe from any thread, so close() may be called
            // repeatedly / off the EDT without effect after the first time.
            SwingUtilities.invokeLater(dialog::dispose);
        }

        @Override
        public void update(long bytesDone, long total) {
            if (total <= 0) {
                return; // size unknown -> leave the indeterminate spinner running
            }
            int pct = (int) Math.max(0, Math.min(100, bytesDone * 100 / total));
            SwingUtilities.invokeLater(() -> {
                if (bar.isIndeterminate()) {
                    bar.setIndeterminate(false);
                    bar.setStringPainted(true); // render the "42%" text
                }
                bar.setValue(pct);
            });
        }
    }
}

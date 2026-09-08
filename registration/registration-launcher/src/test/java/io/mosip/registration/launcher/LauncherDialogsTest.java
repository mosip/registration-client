/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.GraphicsEnvironment;

import static org.junit.Assert.assertNotNull;

/**
 * Exercises the operator-dialog wrappers on the <b>headless</b> path — the only path safe to unit
 * test, since the alternative pops a real (blocking) {@link javax.swing.JOptionPane}. Headless is the
 * mode the launcher runs in unattended / in CI, so this also verifies the
 * {@code GraphicsEnvironment.isHeadless()} guard short-circuits without throwing.
 */
public class LauncherDialogsTest {

    private static String previousHeadless;

    @BeforeClass
    public static void forceHeadless() {
        // Set before the first GraphicsEnvironment query so the JOptionPane branch is never reached
        // and the tests cannot block on a real dialog on a developer machine with a display.
        previousHeadless = System.getProperty("java.awt.headless");
        System.setProperty("java.awt.headless", "true");
    }

    @AfterClass
    public static void restoreHeadless() {
        // Restore the JVM-global property so later tests in the same Surefire JVM aren't forced headless.
        if (previousHeadless == null) {
            System.clearProperty("java.awt.headless");
        } else {
            System.setProperty("java.awt.headless", previousHeadless);
        }
    }

    @Test
    public void error_headless_logsAndReturnsWithoutDialog() {
        Assume.assumeTrue("requires a headless JVM", GraphicsEnvironment.isHeadless());
        LauncherDialogs.error("an error message");
    }

    @Test
    public void info_headless_logsAndReturnsWithoutDialog() {
        Assume.assumeTrue("requires a headless JVM", GraphicsEnvironment.isHeadless());
        LauncherDialogs.info("an informational message");
    }

    @Test
    public void progress_headless_returnsNoOpHandleThatClosesQuietly() {
        Assume.assumeTrue("requires a headless JVM", GraphicsEnvironment.isHeadless());
        LauncherDialogs.ProgressHandle handle = LauncherDialogs.progress("preparing the update");
        // On the headless path a non-null no-op handle is returned; update() and close() are all safe
        // to call (idempotently, off any real window) from the test thread without throwing or blocking.
        assertNotNull(handle);
        handle.update(50, 100); // determinate update -> safe no-op when headless
        handle.update(0, -1);   // unknown-size update -> safe no-op
        handle.close();
        handle.close();
    }
}
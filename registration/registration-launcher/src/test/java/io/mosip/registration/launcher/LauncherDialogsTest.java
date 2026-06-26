package io.mosip.registration.launcher;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.GraphicsEnvironment;

/**
 * Exercises the operator-dialog wrappers on the <b>headless</b> path — the only path safe to unit
 * test, since the alternative pops a real (blocking) {@link javax.swing.JOptionPane}. Headless is the
 * mode the launcher runs in unattended / in CI, so this also verifies the
 * {@code GraphicsEnvironment.isHeadless()} guard short-circuits without throwing.
 */
public class LauncherDialogsTest {

    @BeforeClass
    public static void forceHeadless() {
        // Set before the first GraphicsEnvironment query so the JOptionPane branch is never reached
        // and the tests cannot block on a real dialog on a developer machine with a display.
        System.setProperty("java.awt.headless", "true");
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
}
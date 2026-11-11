package registrationtest.utility;

import java.io.IOException;

/**
 * Runs the client on JavaFX Monocle's headless platform so the automation needs no visible
 * window, unlocked screen or real mouse/keyboard.
 * <p>
 * {@code headless} in config.properties ({@code Y}, {@code yes} or {@code true}) turns it on.
 * {@code -Dheadless=true} or {@code -Dheadless=false} overrides the file. The launch scripts
 * must still patch {@code javafx.graphics} with Monocle before the JVM starts.
 */
public final class HeadlessMode {

    private static Boolean enabled;

    private HeadlessMode() {
    }

    public static boolean isEnabled() {
        if (enabled == null) {
            enabled = resolve();
        }
        return enabled;
    }

    /** Must run before the JavaFX toolkit starts. */
    public static void configure() {
        if (!isEnabled()) {
            return;
        }
        System.setProperty("headless", "true");
        System.setProperty("testfx.robot", "glass");
        System.setProperty("testfx.headless", "true");
        System.setProperty("glass.platform", "Monocle");
        System.setProperty("monocle.platform", "Headless");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("headless.geometry", System.getProperty("headless.geometry", "1920x1080-32"));
    }

    private static boolean resolve() {
        String flag = System.getProperty("headless");
        if (flag != null) {
            return Boolean.parseBoolean(flag);
        }
        try {
            String value = PropertiesUtil.getKeyValue("headless");
            if (value == null) {
                return false;
            }
            String normalized = value.trim();
            return normalized.equalsIgnoreCase("Y")
                    || normalized.equalsIgnoreCase("yes")
                    || normalized.equalsIgnoreCase("true");
        } catch (IOException e) {
            return false;
        }
    }
}

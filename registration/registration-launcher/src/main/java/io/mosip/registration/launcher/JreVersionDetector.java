package io.mosip.registration.launcher;

/**
 * Detects the major Java feature version the launcher is running under, from
 * {@code System.getProperty("java.version")} (as mandated by the migration design — since
 * {@code run.bat} hardcodes {@code jre\bin\javaw} this always reflects the JRE in the app root).
 * <p>
 * Handles both the legacy {@code 1.8.0_x} scheme and the modern {@code 11}, {@code 11.0.3},
 * {@code 21.0.3+9} schemes. Pure JDK so it is safe under the Java 11 JRE.
 */
public final class JreVersionDetector {

    private JreVersionDetector() {
        // utility class
    }

    /**
     * @return the major feature version of the currently running JRE (e.g. 11, 21)
     */
    public static int currentMajorVersion() {
        return majorVersion(System.getProperty("java.version"));
    }

    /**
     * Parses a {@code java.version} string into its major feature version.
     *
     * @param version the raw version string (e.g. {@code "11.0.3"}, {@code "21.0.3+9"}, {@code "1.8.0_292"})
     * @return the major feature version (e.g. 11, 21, 8)
     * @throws IllegalArgumentException if {@code version} is null/blank or cannot be parsed
     */
    public static int majorVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("java.version is null or blank");
        }
        String value = version.trim();
        // strip build ("+9") and pre-release ("-ea") suffixes
        int plus = value.indexOf('+');
        if (plus >= 0) {
            value = value.substring(0, plus);
        }
        int dash = value.indexOf('-');
        if (dash >= 0) {
            value = value.substring(0, dash);
        }
        String[] parts = value.split("\\.");
        try {
            // legacy scheme: 1.8.0_x -> major is the second component
            if ("1".equals(parts[0]) && parts.length > 1) {
                return Integer.parseInt(parts[1]);
            }
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unparseable java.version: " + version, e);
        }
    }
}

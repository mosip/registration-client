package io.mosip.registration.launcher;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JreVersionDetectorTest {

    @Test
    public void majorVersion_modernSingleNumber() {
        assertEquals(11, JreVersionDetector.majorVersion("11"));
        assertEquals(17, JreVersionDetector.majorVersion("17"));
    }

    @Test
    public void majorVersion_modernDotted() {
        assertEquals(11, JreVersionDetector.majorVersion("11.0.3"));
        assertEquals(21, JreVersionDetector.majorVersion("21.0.3"));
    }

    @Test
    public void majorVersion_modernWithBuildSuffix() {
        assertEquals(21, JreVersionDetector.majorVersion("21.0.3+9"));
        assertEquals(11, JreVersionDetector.majorVersion("11-ea"));
    }

    @Test
    public void majorVersion_legacyScheme() {
        assertEquals(8, JreVersionDetector.majorVersion("1.8.0_292"));
        assertEquals(7, JreVersionDetector.majorVersion("1.7.0"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void majorVersion_null_throws() {
        JreVersionDetector.majorVersion(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void majorVersion_blank_throws() {
        JreVersionDetector.majorVersion("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void majorVersion_garbage_throws() {
        JreVersionDetector.majorVersion("not-a-version");
    }

    @Test
    public void currentMajorVersion_matchesRuntime() {
        // Running under JDK 21 in this build, but assert it is at least 11 to stay robust.
        assertTrue(JreVersionDetector.currentMajorVersion() >= 11);
    }
}

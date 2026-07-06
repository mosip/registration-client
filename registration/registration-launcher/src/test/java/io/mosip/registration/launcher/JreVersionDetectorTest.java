/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
    public void currentMajorVersion_matchesRuntimeDelegation() {
        // currentMajorVersion() just delegates to majorVersion(System.getProperty("java.version")) —
        // assert that exact equality so a regression in the delegation is caught (not merely >= 11).
        assertEquals(JreVersionDetector.majorVersion(System.getProperty("java.version")),
                JreVersionDetector.currentMajorVersion());
    }
}

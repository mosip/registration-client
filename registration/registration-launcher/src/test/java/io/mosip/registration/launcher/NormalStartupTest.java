/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NormalStartupTest {

    /**
     * The launcher test classpath has no JavaFX / registration-client jars, so the reflective
     * launch must fail cleanly with a {@link ReflectiveOperationException} (rather than a
     * NoClassDefFoundError or a silent no-op). This verifies the reflection wiring and the
     * declared failure contract that {@code Initialization} relies on.
     */
    @Test
    public void launch_withoutClientOnClasspath_throwsReflectiveOperationException() {
        // launch() sets these JVM-global properties before the (failing) reflective launch — capture and
        // restore them so later tests in the same JVM aren't affected.
        String previousUseSystemProxies = System.getProperty("java.net.useSystemProxies");
        String previousLogbackConfig = System.getProperty("logback.configurationFile");
        try {
            NormalStartup.launch(new String[0]);
            fail("Expected ReflectiveOperationException when client/JavaFX classes are absent");
        } catch (ReflectiveOperationException expected) {
            assertTrue(expected instanceof ClassNotFoundException);
        } finally {
            restore("java.net.useSystemProxies", previousUseSystemProxies);
            restore("logback.configurationFile", previousLogbackConfig);
        }
    }

    private static void restore(String key, String previous) {
        if (previous == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previous);
        }
    }
}

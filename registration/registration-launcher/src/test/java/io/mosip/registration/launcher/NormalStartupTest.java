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
        try {
            NormalStartup.launch(new String[0]);
            fail("Expected ReflectiveOperationException when client/JavaFX classes are absent");
        } catch (ReflectiveOperationException expected) {
            assertTrue(expected instanceof ClassNotFoundException);
        }
    }
}

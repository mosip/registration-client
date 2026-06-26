package io.mosip.registration.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Paths;

/**
 * Step 6 normal startup (design doc): launches the JavaFX {@code ClientApplication} on the Java 21
 * path.
 * <p>
 * The client classes are resolved <b>reflectively</b> so this Java 11 launcher carries no
 * compile-time dependency on {@code registration-client} (a Java 21 jar). This path is only ever
 * reached when the manifest versions match, i.e. the JVM is the post-migration Java 21 runtime that
 * can safely load those classes.
 */
public final class NormalStartup {

    private static final Logger LOGGER = LoggerFactory.getLogger(NormalStartup.class);

    private static final String LAUNCHER_IMPL = "com.sun.javafx.application.LauncherImpl";
    private static final String CLIENT_APPLICATION = "io.mosip.registration.controller.ClientApplication";
    private static final String CLIENT_PRELOADER = "io.mosip.registration.preloader.ClientPreLoader";

    private NormalStartup() {
        // utility class
    }

    /**
     * Reflectively invokes
     * {@code LauncherImpl.launchApplication(ClientApplication.class, ClientPreLoader.class, args)}.
     *
     * @throws ReflectiveOperationException if the client/JavaFX classes are not on the classpath or
     *                                      the launch method cannot be invoked
     */
    public static void launch(String[] args) throws ReflectiveOperationException {
        setSystemProperties();

        Class<?> launcherImpl = Class.forName(LAUNCHER_IMPL);
        Class<?> clientApplication = Class.forName(CLIENT_APPLICATION);
        Class<?> clientPreLoader = Class.forName(CLIENT_PRELOADER);

        Method launchApplication = launcherImpl.getMethod(
                "launchApplication", Class.class, Class.class, String[].class);
        LOGGER.info("Launching {} via {}", CLIENT_APPLICATION, LAUNCHER_IMPL);
        launchApplication.invoke(null, clientApplication, clientPreLoader, args);
    }

    /** Mirrors the system properties the legacy client entry point set before launch. */
    private static void setSystemProperties() {
        System.setProperty("java.net.useSystemProxies", "true");
        // NOTE: file.encoding is intentionally NOT set here — it is read once at JVM boot, so
        // System.setProperty("file.encoding", ...) after startup is a no-op. UTF-8 is established by
        // run.bat's -Dfile.encoding=UTF-8 launch flag (see configure.sh), which is the only place it works.
        try {
            System.setProperty("logback.configurationFile",
                    Paths.get("lib", "logback.xml").toFile().getCanonicalPath());
        } catch (IOException e) {
            LOGGER.warn("Could not resolve logback configuration path", e);
        }
    }
}

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher;

import io.mosip.registration.launcher.common.LauncherLog;

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

    private static final LauncherLog LOGGER = LauncherLog.get(NormalStartup.class);

    private static final String LAUNCHER_IMPL = "com.sun.javafx.application.LauncherImpl";
    private static final String CLIENT_APPLICATION = "io.mosip.registration.controller.ClientApplication";
    private static final String CLIENT_PRELOADER = "io.mosip.registration.preloader.ClientPreLoader";

    private NormalStartup() {
        // utility class
    }

    /**
     * Resolves the client/JavaFX classes and the launch method <b>without</b> starting the
     * application, so a caller can confirm the installed {@code lib/} is loadable before taking any
     * irreversible step (the entry point cleans up the rollback tooling between this call and
     * {@link Launch#start(String[])}).
     * <p>
     * {@code Class.forName} initializes the class it resolves, so a client jar whose static
     * initialization is broken fails here — as an {@link ExceptionInInitializerError} or
     * {@link NoClassDefFoundError}, both {@link LinkageError}s rather than exceptions, which callers
     * must catch explicitly.
     *
     * @throws ReflectiveOperationException if the client/JavaFX classes are not on the classpath or
     *                                      the launch method cannot be resolved
     */
    public static Launch prepare() throws ReflectiveOperationException {
        setSystemProperties();

        Class<?> launcherImpl = Class.forName(LAUNCHER_IMPL);
        Class<?> clientApplication = Class.forName(CLIENT_APPLICATION);
        Class<?> clientPreLoader = Class.forName(CLIENT_PRELOADER);

        Method launchApplication = launcherImpl.getMethod(
                "launchApplication", Class.class, Class.class, String[].class);
        return new Launch(launchApplication, clientApplication, clientPreLoader);
    }

    /**
     * Reflectively invokes
     * {@code LauncherImpl.launchApplication(ClientApplication.class, ClientPreLoader.class, args)}.
     *
     * @throws ReflectiveOperationException if the client/JavaFX classes are not on the classpath or
     *                                      the launch method cannot be invoked
     */
    public static void launch(String[] args) throws ReflectiveOperationException {
        prepare().start(args);
    }

    /** A resolved, ready-to-invoke client launch (see {@link NormalStartup#prepare()}). */
    public static final class Launch {

        private final Method launchApplication;
        private final Class<?> clientApplication;
        private final Class<?> clientPreLoader;

        private Launch(Method launchApplication, Class<?> clientApplication, Class<?> clientPreLoader) {
            this.launchApplication = launchApplication;
            this.clientApplication = clientApplication;
            this.clientPreLoader = clientPreLoader;
        }

        /**
         * Starts the application. Blocks until the JavaFX application exits.
         *
         * @throws ReflectiveOperationException if the launch method cannot be invoked
         */
        public void start(String[] args) throws ReflectiveOperationException {
            LOGGER.info("Launching {} via {}", CLIENT_APPLICATION, LAUNCHER_IMPL);
            launchApplication.invoke(null, clientApplication, clientPreLoader, args);
        }
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

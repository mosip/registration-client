package io.mosip.registration.controller;

import com.sun.javafx.application.LauncherImpl;
import io.mosip.registration.preloader.ClientPreLoader;

import java.nio.file.Path;


public class Initialization {

    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.useSystemProxies", "true");
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("logback.configurationFile", Path.of("lib", "logback.xml").toFile().getCanonicalPath());   //NOSONAR Setting logger configuration file path here.

        LauncherImpl.launchApplication(ClientApplication.class, ClientPreLoader.class, args);
    }
}

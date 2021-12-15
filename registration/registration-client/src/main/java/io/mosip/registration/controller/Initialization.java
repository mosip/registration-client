package io.mosip.registration.controller;

import com.sun.javafx.application.LauncherImpl;
import io.mosip.registration.preloader.ClientPreLoader;

import java.nio.file.Paths;

public class Initialization {

    public static void main(String[] args) {
        System.setProperty("java.net.useSystemProxies", "true");
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("logback.configurationFile", Paths.get(".","lib", "logback.xml").toString());
        LauncherImpl.launchApplication(ClientApplication.class, ClientPreLoader.class, args);
    }
}

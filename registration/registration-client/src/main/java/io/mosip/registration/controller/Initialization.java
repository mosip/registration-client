package io.mosip.registration.controller;

import com.sun.javafx.application.LauncherImpl;
import io.mosip.registration.preloader.ClientPreLoader;

public class Initialization {

    public static void main(String[] args) {
        System.setProperty("java.net.useSystemProxies", "true");
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("logback.configurationFile", "lib/logback.xml");
        LauncherImpl.launchApplication(ClientApplication.class, ClientPreLoader.class, args);
    }
}

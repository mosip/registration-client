package io.mosip.registration.controller;

import com.sun.javafx.application.LauncherImpl;
import io.mosip.registration.preloader.ClientPreLoader;

public class Initialization {

    public static void main(String[] args) {
        LauncherImpl.launchApplication(ClientApplication.class, ClientPreLoader.class, args);
    }
}

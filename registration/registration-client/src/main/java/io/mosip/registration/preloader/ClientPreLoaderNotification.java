package io.mosip.registration.preloader;

import javafx.application.Preloader;

public class ClientPreLoaderNotification implements Preloader.PreloaderNotification {

    private String message;

    public ClientPreLoaderNotification(String statusMessage) {
        setMessage(statusMessage);
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

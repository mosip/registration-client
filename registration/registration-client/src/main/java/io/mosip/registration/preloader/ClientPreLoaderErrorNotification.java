package io.mosip.registration.preloader;

import javafx.application.Preloader;

public class ClientPreLoaderErrorNotification implements Preloader.PreloaderNotification {

    private Throwable cause;

    public ClientPreLoaderErrorNotification(Throwable t) {
        setCause(t);
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }
}

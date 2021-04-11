package io.mosip.registration.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;

public class DeviceException extends BaseCheckedException  {


    /**
     * Constructs a new checked exception
     */
    public DeviceException() {
        super();
    }

    /**
     * Constructs a new checked exception with the specified detail message and
     * error code.
     *
     * @param errorCode
     *            the error code
     * @param errorMessage
     *            the detail message.
     */
    public DeviceException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }

    /**
     * Constructs a new checked exception with the specified detail message and
     * error code.
     *
     * @param errorCode
     *            the error code
     * @param errorMessage
     *            the detail message
     * @param throwable
     *            the specified cause
     */
    public DeviceException(String errorCode, String errorMessage, Throwable throwable) {
        super(errorCode, errorMessage, throwable);
    }
}

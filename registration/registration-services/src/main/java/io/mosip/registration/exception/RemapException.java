package io.mosip.registration.exception;


public class RemapException extends RegBaseCheckedException {


    /**
     * Default constructor
     */
    public RemapException() {
        super();
    }

    /**
     *
     * @param errorCode
     * @param errorMessage
     */
    public RemapException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
}

package io.mosip.registration.exception;


public class PreConditionCheckException extends RegBaseCheckedException {
    /**
     * Default constructor
     */
    public PreConditionCheckException() {
        super();
    }

    /**
     *
     * @param errorCode
     * @param errorMessage
     */
    public PreConditionCheckException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
}

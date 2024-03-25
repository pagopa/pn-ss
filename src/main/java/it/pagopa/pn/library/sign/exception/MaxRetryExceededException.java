package it.pagopa.pn.library.sign.exception;

public class MaxRetryExceededException extends Exception {

    public MaxRetryExceededException(String message) {
        super(message);
    }

    public MaxRetryExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}

package it.pagopa.pnss.common.exception;

public class RequestValidationException extends RuntimeException {

    public RequestValidationException (String string) {
        super(String.format(string));
    }
}

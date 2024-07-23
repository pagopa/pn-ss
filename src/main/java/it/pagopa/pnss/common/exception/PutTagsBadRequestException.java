package it.pagopa.pnss.common.exception;

public class PutTagsBadRequestException extends RuntimeException {

    public PutTagsBadRequestException() {
        super("Bad request during put tags operation");
    }

    public PutTagsBadRequestException(String message) {
        super(message);
    }
}

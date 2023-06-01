package it.pagopa.pnss.common.client.exception;

public class RetentionToIgnoreException extends RuntimeException {

    public RetentionToIgnoreException() {
        super("This retention period has to be ignored");
    }

}

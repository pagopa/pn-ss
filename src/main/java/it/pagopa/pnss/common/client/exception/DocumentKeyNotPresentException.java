package it.pagopa.pnss.common.client.exception;

public class DocumentKeyNotPresentException extends RuntimeException {

    public DocumentKeyNotPresentException(String keyName) {
        super(String.format("Document key not present in DB  '%s'", keyName));
    }
}

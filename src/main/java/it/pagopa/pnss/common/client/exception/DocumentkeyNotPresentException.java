package it.pagopa.pnss.common.client.exception;

public class DocumentkeyNotPresentException extends RuntimeException {

    public DocumentkeyNotPresentException(String keyName ) {
        super(String.format("Docuemnt key present non DB  '%s'", keyName ));
    }
}

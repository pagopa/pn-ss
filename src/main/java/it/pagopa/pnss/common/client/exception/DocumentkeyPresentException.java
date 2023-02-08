package it.pagopa.pnss.common.client.exception;

public class DocumentkeyPresentException extends RuntimeException {

    public DocumentkeyPresentException(String keyName ) {
        super(String.format("Docuemnt key present non DB  '%s'", keyName ));
    }
}

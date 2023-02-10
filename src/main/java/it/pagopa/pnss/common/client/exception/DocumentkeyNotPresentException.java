package it.pagopa.pnss.common.client.exception;

public class DocumentkeyNotPresentException extends RuntimeException {

    public DocumentkeyNotPresentException(String keyName ) {
        super(String.format("Docuemnt key not present in DB  '%s'", keyName ));
    }
}

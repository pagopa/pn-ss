package it.pagopa.pnss.common.client.exception;

public class DocumentTypeNotPresentException extends RuntimeException{

    public DocumentTypeNotPresentException(String key) {
        super(String.format("Document type key not present in DB '%s'", key));
    }
}

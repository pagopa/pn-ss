package it.pagopa.pnss.common.client.exception;

public class DocumentTypeNotPresentException extends RuntimeException{

    private static final long serialVersionUID = -7137960787541574509L;

	public DocumentTypeNotPresentException(String key) {
        super(String.format("Document type key not present in DB '%s'", key));
    }
}

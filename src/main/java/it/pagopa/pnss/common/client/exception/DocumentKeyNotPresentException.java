package it.pagopa.pnss.common.client.exception;

public class DocumentKeyNotPresentException extends RuntimeException {

    private static final long serialVersionUID = -6802642318482917457L;

	public DocumentKeyNotPresentException(String keyName) {
        super(String.format("Document key not present in DB  '%s'", keyName));
    }
}

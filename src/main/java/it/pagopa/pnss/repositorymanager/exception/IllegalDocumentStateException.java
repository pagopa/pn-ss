package it.pagopa.pnss.repositorymanager.exception;

public class IllegalDocumentStateException extends RuntimeException {

    private static final long serialVersionUID = -1030715139591555485L;

	public IllegalDocumentStateException(String message) {
        super(message);
    }
}

package it.pagopa.pnss.common.client.exception;

public class DocumentkeyPresentException extends RuntimeException {

    private static final long serialVersionUID = -7496954963766087384L;

	public DocumentkeyPresentException(String keyName ) {
        super(String.format("Docuemnt key present non DB  '%s'", keyName ));
    }
}

package it.pagopa.pnss.common.client.exception;

public class RetentionException extends RuntimeException {

	private static final long serialVersionUID = 4804911725627168656L;
	
	public RetentionException(String msg) {
        super(String.format("Retention error: %s", msg));
    }

}

package it.pagopa.pnss.common.client.exception;

public class IdentityCheckFailException extends RuntimeException {

	private static final long serialVersionUID = -7904635392640753954L;
	
	public IdentityCheckFailException(String msg) {
		 super(String.format("Client not autorized: '%s'", msg));
    }

}

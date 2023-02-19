package it.pagopa.pnss.common.client.exception;

public class HeaderNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -7904635392640753954L;
	
	public HeaderNotFoundException(String header) {
        super(String.format("Headers '%s' not found", header));
    }

}

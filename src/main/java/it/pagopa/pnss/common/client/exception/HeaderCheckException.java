package it.pagopa.pnss.common.client.exception;

public class HeaderCheckException extends RuntimeException {

	private static final long serialVersionUID = -7904635392640753954L;
	
	public HeaderCheckException(String msg) {
        super(msg);
    }
	
	public HeaderCheckException(String header, String msg) {
        super(String.format("Header '%s':  %s", header, msg));
    }

}

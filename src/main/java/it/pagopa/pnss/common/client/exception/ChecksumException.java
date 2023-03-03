package it.pagopa.pnss.common.client.exception;

public class ChecksumException extends RuntimeException {

	private static final long serialVersionUID = 6662744847428261285L;
	
	public ChecksumException(String msg) {
		super(String.format("Checksum error = %s", msg));
	}

}

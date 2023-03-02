package it.pagopa.pnss.common.client.exception;

public class PatchDocumentExcetpion extends RuntimeException {

	private static final long serialVersionUID = -3853015174582826482L;

	public PatchDocumentExcetpion(String message) {
		super(String.format("Patch Document Error: %s", message));
	}

}

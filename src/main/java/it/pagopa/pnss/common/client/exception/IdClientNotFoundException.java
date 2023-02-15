package it.pagopa.pnss.common.client.exception;

public class IdClientNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -738427416255539921L;

	public IdClientNotFoundException(String idClient) {
        super(String.format("Client id '%s' is unauthorized", idClient));
    }
}

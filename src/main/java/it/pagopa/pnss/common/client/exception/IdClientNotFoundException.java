package it.pagopa.pnss.common.client.exception;

public class IdClientNotFoundException extends RuntimeException {

    public IdClientNotFoundException(String idClient) {
        super(String.format("Client id '%s' is unauthorized", idClient));
    }
}

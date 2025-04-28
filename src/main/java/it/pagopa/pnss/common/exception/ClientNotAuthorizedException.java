package it.pagopa.pnss.common.exception;

public class ClientNotAuthorizedException extends RuntimeException {

    public ClientNotAuthorizedException (String cxId) {
        super(String.format("Client %s is not authorized to perform this operation.",cxId));
    }
}

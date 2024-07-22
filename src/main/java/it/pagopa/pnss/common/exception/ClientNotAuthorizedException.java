package it.pagopa.pnss.common.exception;

public class ClientNotAuthorizedException extends RuntimeException {

    public ClientNotAuthorizedException (String cxId) {
        super(String.format("Client "+ cxId +" is not authorized to perform this operation."));
    }
}

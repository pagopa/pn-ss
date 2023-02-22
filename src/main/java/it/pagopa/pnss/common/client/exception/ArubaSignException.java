package it.pagopa.pnss.common.client.exception;

public class ArubaSignException extends RuntimeException {

    public ArubaSignException(String keyName ) {
        super(String.format("Problem sign document witk keyName '%s'", keyName ));
    }
}
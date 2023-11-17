package it.pagopa.pnss.common.client.exception;

public class ArubaSignExceptionLimitCall extends RuntimeException {

    public ArubaSignExceptionLimitCall(String keyName ) {
        super(String.format("Problem sign document with keyName '%s'", keyName ));
    }
}

package it.pagopa.pn.library.sign.exception.aruba;

public class ArubaSignExceptionLimitCall extends RuntimeException {

    public ArubaSignExceptionLimitCall(String keyName ) {
        super(String.format("Problem sign document with keyName '%s'", keyName ));
    }
}

package it.pagopa.pn.library.sign.exception;

public class PnSpapiPermanentErrorException extends Exception {
    public PnSpapiPermanentErrorException(String message) {
        super(message);
    }

    public PnSpapiPermanentErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}

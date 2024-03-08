package it.pagopa.pn.library.sign.exception;

public class PnSpapiTemporaryErrorException extends Exception {
    public PnSpapiTemporaryErrorException(String message) {
        super(message);
    }

    public PnSpapiTemporaryErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}

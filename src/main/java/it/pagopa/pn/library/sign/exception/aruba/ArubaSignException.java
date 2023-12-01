package it.pagopa.pn.library.sign.exception.aruba;

public class ArubaSignException extends RuntimeException {

    public ArubaSignException() {
        super("Error while invoking Aruba services for digital signature");
    }
}

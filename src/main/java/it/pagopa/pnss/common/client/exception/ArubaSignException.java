package it.pagopa.pnss.common.client.exception;

public class ArubaSignException extends RuntimeException {

    public ArubaSignException() {
        super("Error while invoking Aruba services for digital signature");
    }
}

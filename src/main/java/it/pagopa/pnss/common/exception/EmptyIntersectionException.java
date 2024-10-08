package it.pagopa.pnss.common.exception;

public class EmptyIntersectionException extends RuntimeException {

    public EmptyIntersectionException() {
        super("The intersection of fileKeys is empty");
    }
}

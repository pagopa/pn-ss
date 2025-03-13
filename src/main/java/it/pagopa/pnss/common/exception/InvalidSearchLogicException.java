package it.pagopa.pnss.common.exception;

public class InvalidSearchLogicException extends RuntimeException {

    public InvalidSearchLogicException(String logic) {
        super(String.format("Invalid search logic: %s", logic));
    }

}

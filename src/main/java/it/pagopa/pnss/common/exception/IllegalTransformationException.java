package it.pagopa.pnss.common.exception;

public class IllegalTransformationException extends RuntimeException {

    public IllegalTransformationException(String fileKey) {
        super(String.format("Document '%s' does not have a valid transformation type", fileKey));
    }

}

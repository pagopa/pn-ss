package it.pagopa.pnss.common.exception;

public class InvalidStatusTransformationException extends RuntimeException {

    public InvalidStatusTransformationException(String fileKey) {
        super(String.format("Current status is not valid for transformation for document '%s'", fileKey));
    }

}

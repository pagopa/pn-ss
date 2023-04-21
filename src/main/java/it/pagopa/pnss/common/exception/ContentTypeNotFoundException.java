package it.pagopa.pnss.common.exception;

public class ContentTypeNotFoundException extends RuntimeException {

    public ContentTypeNotFoundException(String contentType) {
        super(String.format("The contentType %s is not valid", contentType));
    }

}

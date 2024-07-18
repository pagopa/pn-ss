package it.pagopa.pnss.common.client.exception;

public class TagKeyValueNotPresentException extends RuntimeException {


    public TagKeyValueNotPresentException(String keyName) {
        super(String.format("Tag key value '%s' not present in DB", keyName));
    }
}

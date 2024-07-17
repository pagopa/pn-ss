package it.pagopa.pnss.repositorymanager.exception;

public class TagKeyValueNotPresentException extends RuntimeException {


    public TagKeyValueNotPresentException(String keyName) {
        super(String.format("Tag key value '%s' not present in DB", keyName));
    }
}

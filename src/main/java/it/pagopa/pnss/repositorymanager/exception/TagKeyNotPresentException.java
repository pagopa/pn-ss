package it.pagopa.pnss.repositorymanager.exception;

public class TagKeyNotPresentException extends RuntimeException {


    public TagKeyNotPresentException(String keyName) {
        super(String.format("Tag key not present in DB  '%s'", keyName));
    }
}

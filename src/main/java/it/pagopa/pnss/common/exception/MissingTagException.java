package it.pagopa.pnss.common.exception;

public class MissingTagException extends RuntimeException {

    public MissingTagException(String tagKey) {
        super(String.format("Tag '%s' not found in the indexing configuration", tagKey));
    }
}

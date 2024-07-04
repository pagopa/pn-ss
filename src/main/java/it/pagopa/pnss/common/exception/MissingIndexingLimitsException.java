package it.pagopa.pnss.common.exception;

public class MissingIndexingLimitsException extends RuntimeException {

    public MissingIndexingLimitsException() {
        super("Some of the required limits are missing from the indexing configuration");
    }

}

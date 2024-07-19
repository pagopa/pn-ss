package it.pagopa.pnss.common.exception;

public class IndexingLimitException extends RuntimeException {

    public IndexingLimitException(String limitName, int currentValue, long maxValue) {
        super(String.format("Limit '%s' reached. Current value: %d. Max value: %d", limitName, currentValue, maxValue));
    }

}

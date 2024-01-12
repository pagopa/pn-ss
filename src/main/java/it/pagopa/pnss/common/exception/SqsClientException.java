package it.pagopa.pnss.common.exception;

public class SqsClientException extends RuntimeException {

    public SqsClientException(String queueName) {
        super(String.format("An error occurred during client operation on '%s' queue", queueName));
    }
}

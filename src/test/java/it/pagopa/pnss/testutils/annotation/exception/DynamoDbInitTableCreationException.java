package it.pagopa.pnss.testutils.annotation.exception;

public class DynamoDbInitTableCreationException extends RuntimeException{

    public DynamoDbInitTableCreationException(String tableName) {
        super(String.format("Error during %s dynamo db table creation", tableName));
    }
}

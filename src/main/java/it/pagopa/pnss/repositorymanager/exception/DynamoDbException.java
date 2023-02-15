package it.pagopa.pnss.repositorymanager.exception;

import java.io.Serializable;

public class DynamoDbException extends RuntimeException implements Serializable {

	private static final long serialVersionUID = -3419027628817960755L;

	public DynamoDbException() {
        super("Error interacting with DynamoDb");
    }
}

package it.pagopa.pnss.repositorymanager.exception;

import java.io.Serializable;

public class ItemDoesNotExist extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 1441959267059588702L;

    public ItemDoesNotExist(String partitionKey) {
        super(String.format("Item with partition key '%s' doesn't exist", partitionKey));
    }
}

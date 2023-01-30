package it.pagopa.pnss.repositoryManager.exception;

import java.io.Serializable;

public class ItemAlreadyPresent extends RuntimeException implements Serializable {

	private static final long serialVersionUID = 1441959267059588702L;

	public ItemAlreadyPresent(String idClient) {
        super(String.format("Item with Id %s already exists", idClient));
    }
}

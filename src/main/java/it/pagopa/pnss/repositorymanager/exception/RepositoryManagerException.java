package it.pagopa.pnss.repositorymanager.exception;

import java.io.Serializable;

public class RepositoryManagerException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = -4751475694166796479L;

	public RepositoryManagerException() {
        super("Generic exception in RepositoryManagerService");
    }
	
	public RepositoryManagerException(String msg) {
        super(msg);
    }
}

package it.pagopa.pnss.repositorymanager.exception;

import java.io.Serializable;

public class BucketException extends RuntimeException implements Serializable {

	private static final long serialVersionUID = 925552563573243078L;

	public BucketException() {
        super("Generic exception in BucketService");
    }
	
	public BucketException(String msg) {
        super(msg);
    }

}

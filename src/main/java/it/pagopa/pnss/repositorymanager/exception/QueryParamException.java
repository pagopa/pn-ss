package it.pagopa.pnss.repositorymanager.exception;

public class QueryParamException extends RuntimeException {

	private static final long serialVersionUID = 8141854326941839520L;
	
	public QueryParamException(String msg) {
		super(String.format("Query Param error = %s", msg));
	}

}

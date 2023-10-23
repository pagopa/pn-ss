package it.pagopa.pnss.common.client.exception;

import org.springframework.core.NestedRuntimeException;

public class S3BucketException extends NestedRuntimeException
{

    private static final long serialVersionUID = 5366578194161754839L;
    
	public S3BucketException(String desc ) {
        super(String.format("Problem with S3 Bucket: %s", desc));
    }
	
    public static class BucketNotPresentException extends RuntimeException {

        private static final long serialVersionUID = 622766874338902616L;

		public BucketNotPresentException(String idRequest) {
            super(String.format("Id request '%s' already present", idRequest));
        }
    }
    
    public static class NoSuchKeyException extends RuntimeException {

        private static final long serialVersionUID = 1351915341973921122L;

		public NoSuchKeyException(String keyFile) {
            super(String.format("The key '%s' is not present in bucket ", keyFile));
        }
    }

}

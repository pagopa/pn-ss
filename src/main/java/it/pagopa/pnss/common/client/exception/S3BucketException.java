package it.pagopa.pnss.common.client.exception;

import org.springframework.core.NestedRuntimeException;

public class S3BucketException extends NestedRuntimeException
{

    public S3BucketException(String desc ) {
        super(String.format("Problem with S3 Bucket'", desc ));
    }
    public static class BucketNotPresentException extends RuntimeException {

        public BucketNotPresentException(String idRequest) {
            super(String.format("Id request '%s' already present", idRequest));
        }
    }
    public static class NoSuchKeyException extends RuntimeException {

        public NoSuchKeyException(String keyFile) {
            super(String.format("The key '%s' is not present in bucket ", keyFile));
        }
    }

}

package it.pagopa.pnss.common.exception;

import software.amazon.awssdk.services.s3.endpoints.internal.Value;

public class RequestValidationException extends RuntimeException {

    public RequestValidationException (String string) {
        super(String.format(string));
    }
}

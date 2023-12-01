package it.pagopa.pnss.common.exception;

public class RestoreRequestDateNotFound extends RuntimeException {

    public RestoreRequestDateNotFound(String headerName) {
        super("Header '' has not been found in http response.");
    }

}

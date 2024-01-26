package it.pagopa.pnss.repositorymanager.exception;

public class MissingTagException extends RuntimeException {

    public MissingTagException(String fileKey) {
        super(String.format("S3 object '%s' lacks any tags.", fileKey));
    }

}

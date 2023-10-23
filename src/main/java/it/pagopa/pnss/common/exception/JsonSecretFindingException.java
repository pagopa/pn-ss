package it.pagopa.pnss.common.exception;

public class JsonSecretFindingException extends RuntimeException{

    public <T> JsonSecretFindingException(Class<T> classToMap) {
        super(String.format("There was an error finding JSON secret of class %s", classToMap.getSimpleName()));
    }
}

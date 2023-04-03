package it.pagopa.pnss.common.exception;

public class PutEventsRequestEntryException extends RuntimeException{

    public <T> PutEventsRequestEntryException(Class<T> classToMap) {
        super(String.format("There was an error creating event in class %s", classToMap.getSimpleName()));
    }
}

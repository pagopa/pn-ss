package it.pagopa.pnss.bucketManager.exception;

public class EventNameNotFoundException extends RuntimeException{
    public EventNameNotFoundException() {
        super(("The Event name record is not correct"));
    }
}



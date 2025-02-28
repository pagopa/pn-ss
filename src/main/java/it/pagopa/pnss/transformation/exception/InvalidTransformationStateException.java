package it.pagopa.pnss.transformation.exception;

public class InvalidTransformationStateException extends RuntimeException {

    public InvalidTransformationStateException(String message) {
        super(message);
    }

    /**
     * Signals that the status associated with the "Transformation-xxx" tag is not recognized
     */
    public static class StatusNotRecognizedException extends InvalidTransformationStateException {

        public StatusNotRecognizedException(String tagState) {
            super(String.format("Status '%s' in transformation tag is not valid", tagState));
        }
    }

    /**
     * Signals that the object to transform has no transformation tag associated with it
     */
    public static class StatusNotFoundException extends InvalidTransformationStateException {

        public StatusNotFoundException() {
            super("The object has no transformation tag associated with it. Invalid state.");
        }
    }


}

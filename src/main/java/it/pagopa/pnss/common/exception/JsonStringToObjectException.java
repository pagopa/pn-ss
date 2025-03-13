package it.pagopa.pnss.common.exception;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

public class JsonStringToObjectException extends RuntimeException {

    public <T> JsonStringToObjectException(String jsonString, Class<T> classToMap, String reason) {
        super(String.format("There was an error converting this JSON string into class %s ↓%n%s Reason: %s", classToMap.getSimpleName(), jsonString, reason));
    }

    public <T> JsonStringToObjectException(String jsonString, TypeReference<T> typeReference, String reason) {
        super(String.format("There was an error converting this JSON string into type %s ↓%n%s. Reason: %s", typeReference.getType(), jsonString, reason));
    }

    public <T> JsonStringToObjectException(String jsonString, Class<T> classToMap, List<String> violations) {
        super(String.format("There was an error converting this JSON string into class %s ↓%n%s. Violations: %s", classToMap.getSimpleName(), jsonString, violations));
    }

    public <T> JsonStringToObjectException(String jsonString, TypeReference<T> typeReference, List<String> violations) {
        super(String.format("There was an error converting this JSON string into type %s ↓%n%s. Violations: %s", typeReference.getType(), jsonString, violations));
    }

}


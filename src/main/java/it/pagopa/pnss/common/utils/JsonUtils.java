package it.pagopa.pnss.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnss.common.exception.JsonStringToObjectException;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.List;
import java.util.Set;

@Component
public class JsonUtils {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public JsonUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    public <T> T convertJsonStringToObject(String jsonString, Class<T> classToMap) throws JsonStringToObjectException {
        try {
            T obj = objectMapper.readValue(jsonString, classToMap);
            // Validazione dell'oggetto
            Set<ConstraintViolation<T>> violations = validator.validate(obj);
            if (!violations.isEmpty()) {
                List<String> violationsList = violations.stream().map(tConstraintViolation -> tConstraintViolation.getPropertyPath() + " - " + tConstraintViolation.getMessage()).toList();
                throw new JsonStringToObjectException(jsonString, classToMap, violationsList);
            }
            return obj;
        } catch (JsonProcessingException e) {
            throw new JsonStringToObjectException(jsonString, classToMap, e.getMessage());
        }
    }

}

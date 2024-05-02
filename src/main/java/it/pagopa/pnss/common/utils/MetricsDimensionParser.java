package it.pagopa.pnss.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricsDimensionParser {

    private final ObjectMapper objectMapper;

    public MetricsDimensionParser() {
        this.objectMapper = new ObjectMapper();
    }

    @SneakyThrows(JsonProcessingException.class)
    public Map<String, Map<String, List<Long>>> parseSignDimensionJson(String dimensionsJson) {
        // Creazione di un TypeReference per indicare il tipo di mappa desiderato
        TypeReference<Map<String, Map<String, List<Long>>>> typeRef = new TypeReference<>() {};

        // Deserializzazione del JSON nella mappa
        return objectMapper.readValue(dimensionsJson, typeRef);

    }

}

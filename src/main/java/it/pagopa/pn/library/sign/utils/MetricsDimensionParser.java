package it.pagopa.pn.library.sign.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MetricsDimensionParser {

    private final ObjectMapper objectMapper;

    public MetricsDimensionParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Map<String, List<Long>>> parseSignDimensionJson(String dimensionsJson) {
        return new HashMap<>();
    }

}

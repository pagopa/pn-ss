package it.pagopa.pn.library.sign.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTestWebEnv
class MetricsDimensionParserTest {

    @Autowired
    private MetricsDimensionParser metricsDimensionParser;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testParseSignDimensionJsonOk() throws IOException {
        String dimensionsJsonSchema = objectMapper.readTree(getClass().getClassLoader().getResource("json/sign-dimensions-schema-test.json")).toString();
        Map<String, Map<String, List<Long>>> dimensionsSchema = metricsDimensionParser.parseSignDimensionJson(dimensionsJsonSchema);
        Assertions.assertNotNull(dimensionsSchema);
        Assertions.assertFalse(dimensionsSchema.isEmpty());
    }

    @Test
    void testParseSignDimensionJsonKo() throws IOException {
        String dimensionsJsonSchema = objectMapper.readTree(getClass().getClassLoader().getResource("json/bad-sign-dimensions-schema-test.json")).toString();
        Assertions.assertThrows(RuntimeException.class, () -> metricsDimensionParser.parseSignDimensionJson(dimensionsJsonSchema));
    }

}

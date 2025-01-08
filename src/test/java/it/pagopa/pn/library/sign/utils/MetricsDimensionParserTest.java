package it.pagopa.pn.library.sign.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnss.common.utils.MetricsDimensionParser;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@SpringBootTestWebEnv
@CustomLog
class MetricsDimensionParserTest {

    private final MetricsDimensionParser metricsDimensionParser = new MetricsDimensionParser();
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testParseSignDimensionJsonOk() throws IOException {
        String dimensionsJsonSchema = objectMapper.readTree(getClass().getClassLoader().getResource("json/sign-dimensions-schema-test.json")).toString();
        Map<String, Map<String, List<Long>>> dimensionsSchema = metricsDimensionParser.parseSignDimensionJson(dimensionsJsonSchema);
        log.info("dimensionsSchema: {}", dimensionsSchema);
        Assertions.assertNotNull(dimensionsSchema);
        Assertions.assertFalse(dimensionsSchema.isEmpty());
        Assertions.assertEquals(3, dimensionsSchema.size());
        Assertions.assertAll(Stream.of("PADES", "XADES", "CADES")
                .map(signType -> () -> {
                    Assertions.assertTrue(dimensionsSchema.containsKey(signType));
                    Assertions.assertFalse(dimensionsSchema.get(signType).isEmpty());
                    Assertions.assertEquals(3, dimensionsSchema.get(signType).size());
                }));
    }

    @Test
    void testParseSignDimensionJsonKo() throws IOException {
        String dimensionsJsonSchema = objectMapper.readTree(getClass().getClassLoader().getResource("json/bad-sign-dimensions-schema-test.json")).toString();
        Assertions.assertThrows(Exception.class, () -> metricsDimensionParser.parseSignDimensionJson(dimensionsJsonSchema));
    }

}

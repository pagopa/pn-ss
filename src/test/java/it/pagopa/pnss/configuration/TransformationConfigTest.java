package it.pagopa.pnss.configuration;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTestWebEnv
class TransformationConfigTest {
    @Autowired
    private TransformationConfig transformationConfig;

    @ParameterizedTest
    @ValueSource(strings = {"DUMMY", "SIGN_AND_TIMEMARK", "SIGN", "RASTER"})
    void getTransformationQueueName_Ok(String transformation) {
        String queueName = transformationConfig.getTransformationQueueName(transformation);
        assertNotNull(queueName);
        assertFalse(queueName.isEmpty());
    }

    @Test
    void getTransformationQueueName_Ko() {
        assertThrows(IllegalArgumentException.class, () -> transformationConfig.getTransformationQueueName("FAKE"));
    }

}

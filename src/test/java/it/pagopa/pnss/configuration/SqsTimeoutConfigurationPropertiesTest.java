package it.pagopa.pnss.configuration;

import it.pagopa.pnss.configurationproperties.SqsTimeoutConfigurationProperties;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTestWebEnv
class SqsTimeoutConfigurationPropertiesTest {

    @Autowired
    private SqsTimeoutConfigurationProperties properties;

    @Test
    void loadPropertiesTest() {
        assertThat(properties.getPercent()).isEqualTo(10);
        assertThat(properties.getManagedQueues()).containsExactly("queue1", "queue2", "queue3");
        assertThat(properties.getDefaultSeconds()).isEqualTo(86400);
    }
}

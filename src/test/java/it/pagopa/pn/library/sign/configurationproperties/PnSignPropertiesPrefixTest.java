package it.pagopa.pn.library.sign.configurationproperties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static org.assertj.core.api.Assertions.assertThat;

class PnSignPropertiesPrefixTest {

    @Test
    void pnSignRetryStrategyProperties_shouldUseNormalizedPnSsPrefix() {
        ConfigurationProperties annotation = PnSignRetryStrategyProperties.class.getAnnotation(ConfigurationProperties.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.prefix()).isEqualTo("pn.ss.sign.retry.strategy");
    }

    @Test
    void pnSignServiceConfigurationProperties_shouldUseNormalizedPnSsPrefix() {
        ConfigurationProperties annotation = PnSignServiceConfigurationProperties.class.getAnnotation(ConfigurationProperties.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.prefix()).isEqualTo("pn.ss.sign");
    }
}

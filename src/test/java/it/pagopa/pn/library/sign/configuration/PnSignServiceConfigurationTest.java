package it.pagopa.pn.library.sign.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PnSignServiceConfigurationTest {

    @Test
    void namirialPropertiesKeySet_shouldReferenceNormalizedPnSsPrefix() {
        PnSignServiceConfiguration config = new PnSignServiceConfiguration();

        @SuppressWarnings("unchecked")
        Set<String> keys = (Set<String>) ReflectionTestUtils.getField(config, "namirialPropertiesKeySet");

        assertThat(keys).containsExactlyInAnyOrder(
                "pn.ss.sign.namirial.address",
                "pn.ss.sign.namirial.max-connections",
                "pn.ss.sign.namirial.pending-acquire-timeout");
    }
}

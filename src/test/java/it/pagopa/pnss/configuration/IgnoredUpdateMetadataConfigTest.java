package it.pagopa.pnss.configuration;

import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SpringBootTestWebEnv
public class IgnoredUpdateMetadataConfigTest extends IgnoredUpdateMetadataConfigTestSetup {
    @Test
    void testRefreshIgnoredUpdateMetadataListOk() {
        Flux<String> fluxToTest = ignoredUpdateMetadataConfig.refreshIgnoredUpdateMetadataList();
        StepVerifier.create(fluxToTest).expectNextCount(5).verifyComplete();
    }

}

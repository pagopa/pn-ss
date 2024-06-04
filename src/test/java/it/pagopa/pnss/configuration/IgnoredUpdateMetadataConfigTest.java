package it.pagopa.pnss.configuration;

import it.pagopa.pnss.utils.IgnoredUpdateMetadataConfigTestSetup;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
@SpringBootTestWebEnv
@CustomLog
class IgnoredUpdateMetadataConfigTest extends IgnoredUpdateMetadataConfigTestSetup {

    @Autowired
    private IgnoredUpdateMetadataConfig ignoredUpdateMetadataConfig;

    @Test
    void testRefreshIgnoredUpdateMetadataListOk() {
        Mono<Integer> fluxToTest = ignoredUpdateMetadataConfig.refreshIgnoredUpdateMetadataList();
        StepVerifier.create(fluxToTest).expectNextMatches(size -> size.equals(5)).verifyComplete();
    }

}

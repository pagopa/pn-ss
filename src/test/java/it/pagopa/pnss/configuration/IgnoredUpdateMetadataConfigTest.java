package it.pagopa.pnss.configuration;

import it.pagopa.pnss.utils.IgnoredUpdateMetadataConfigTestSetup;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
@SpringBootTestWebEnv
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
@CustomLog
class IgnoredUpdateMetadataConfigTest extends IgnoredUpdateMetadataConfigTestSetup {

    @Autowired
    private IgnoredUpdateMetadataConfig ignoredUpdateMetadataConfig;

    @Test
    @Order(1)
    void testRefreshIgnoredUpdateMetadataListOk() {
        Mono<Integer> fluxToTest = ignoredUpdateMetadataConfig.refreshIgnoredUpdateMetadataList();
        StepVerifier.create(fluxToTest).expectNextMatches(size -> size.equals(5)).verifyComplete();
    }

    @Test
    @Order(2)
    void testRefreshIgnoredUpdateMetadataList_NonUpdatedFile() {
        Mono<Integer> fluxToTest = ignoredUpdateMetadataConfig.refreshIgnoredUpdateMetadataList();
        StepVerifier.create(fluxToTest).verifyComplete();
    }

    @Test
    @Order(3)
    void testRefreshIgnoredUpdateMetadataList_NonExistentFileKey() {
        ReflectionTestUtils.setField(ignoredUpdateMetadataConfig, "ignoredUpdateMetadataFileName", "nonExistentFileKey");
        Mono<Integer> fluxToTest = ignoredUpdateMetadataConfig.refreshIgnoredUpdateMetadataList();
        StepVerifier.create(fluxToTest).verifyComplete();
    }

}

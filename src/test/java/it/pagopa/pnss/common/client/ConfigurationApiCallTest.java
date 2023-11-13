package it.pagopa.pnss.common.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentTypeResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfiguration;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

@SpringBootTestWebEnv
public class ConfigurationApiCallTest {

    private static MockWebServer mockBackEnd;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private ConfigurationApiCall configurationApiCall;

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        System.setProperty("internal.base.url", String.format("http://localhost:%s", mockBackEnd.getPort()));
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void getDocumentConfigsOk() throws JsonProcessingException {
        mockBackEnd.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(new DocumentTypesConfigurations())).addHeader("Content-Type", "application/json"));

        Mono<DocumentTypesConfigurations> getDocumentsConfigs = configurationApiCall.getDocumentsConfigs("clientID", "apiKey");
        StepVerifier.create(getDocumentsConfigs).expectNextCount(1).verifyComplete();
    }

    @Test
    void getCurrentClientConfigOk() {
        Assertions.assertNull(configurationApiCall.getCurrentClientConfig("clientId"));
    }

}

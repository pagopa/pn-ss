package it.pagopa.pnss.common.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentTypeResponse;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
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
public class DocTypesClientCallTest {

    private static MockWebServer mockBackEnd;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private DocTypesClientCall docTypesClientCall;

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
    void getDocTypesOk() throws JsonProcessingException {
        mockBackEnd.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(new DocumentTypeResponse())).addHeader("Content-Type", "application/json"));

        Mono<DocumentTypeResponse> getDocTypes = docTypesClientCall.getdocTypes("docTypes");
        StepVerifier.create(getDocTypes).expectNextCount(1).verifyComplete();
    }

    @Test
    void getDocTypesNotFound() {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(404));

        Mono<DocumentTypeResponse> getDocTypes = docTypesClientCall.getdocTypes("docTypes");
        StepVerifier.create(getDocTypes).expectError(DocumentTypeNotPresentException.class).verify();
    }

    @Test
    void postdocTypesOk() {
        Assertions.assertNull(docTypesClientCall.postdocTypes(new DocumentType()));
    }

    @Test
    void updateDocTypesOk() {
        Assertions.assertNull(docTypesClientCall.updatedocTypes(new DocumentType()));
    }

    @Test
    void deleteDocTypesOk() {
        Assertions.assertNull(docTypesClientCall.deletedocTypes("docType"));
    }

}

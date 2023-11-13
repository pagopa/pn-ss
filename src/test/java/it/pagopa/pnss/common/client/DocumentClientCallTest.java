package it.pagopa.pnss.common.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentInput;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Error;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.DocumentkeyPresentException;
import it.pagopa.pnss.common.exception.PatchDocumentException;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.io.IOException;
import static software.amazon.awssdk.http.HttpStatusCode.NOT_FOUND;

@SpringBootTestWebEnv
@Slf4j
public class DocumentClientCallTest {

    private static MockWebServer mockBackEnd;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private DocumentClientCall documentClientCall;

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
    void getDocumentOk() throws JsonProcessingException {
        DocumentResponse documentResponse = new DocumentResponse();
        mockBackEnd.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(documentResponse)).addHeader("Content-Type", "application/json"));

        Mono<DocumentResponse> getDocument = documentClientCall.getDocument("fileKey");
        StepVerifier.create(getDocument).expectNextCount(1).verifyComplete();
    }

    @Test
    void getDocumentNotFound() {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(404));

        Mono<DocumentResponse> getDocument = documentClientCall.getDocument("fileKey");
        StepVerifier.create(getDocument).expectError(DocumentKeyNotPresentException.class).verify();
    }

    @Test
    void postDocumentOk() throws JsonProcessingException {
        DocumentInput documentInput = new DocumentInput();
        DocumentResponse documentResponse = new DocumentResponse();
        mockBackEnd.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(documentResponse)).addHeader("Content-Type", "application/json"));

        Mono<DocumentResponse> postDocument = documentClientCall.postDocument(documentInput);
        StepVerifier.create(postDocument).expectNextCount(1).verifyComplete();
    }

    @Test
    void postDocumentForbidden() {
        DocumentInput documentInput = new DocumentInput();
        mockBackEnd.enqueue(new MockResponse().setResponseCode(403));

        Mono<DocumentResponse> postDocument = documentClientCall.postDocument(documentInput);
        StepVerifier.create(postDocument).expectError(DocumentkeyPresentException.class).verify();
    }

    @Test
    void patchDocumentOk() throws JsonProcessingException {
        DocumentResponse documentResponse = new DocumentResponse();
        DocumentChanges documentChanges = new DocumentChanges();
        mockBackEnd.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(documentResponse)).addHeader("Content-Type", "application/json"));

        Mono<DocumentResponse> postDocument = documentClientCall.patchDocument("", "", "fileKey", documentChanges);
        StepVerifier.create(postDocument).expectNextCount(1).verifyComplete();
    }

    @Test
    void patchDocumentBadRequest() throws JsonProcessingException {
        DocumentChanges documentChanges = new DocumentChanges();
        DocumentResponse documentResponse = new DocumentResponse();
        documentResponse.setError(new Error().code("403").description("Forbidden"));
        mockBackEnd.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(documentResponse)).addHeader("Content-Type", "application/json").setResponseCode(400));

        Mono<DocumentResponse> postDocument = documentClientCall.patchDocument("", "", "fileKey", documentChanges);
        StepVerifier.create(postDocument).expectErrorMatches(throwable -> throwable instanceof PatchDocumentException && ((PatchDocumentException) throwable).getStatusCode().equals(HttpStatus.FORBIDDEN)).verify();
    }

    @Test
    void patchDocumentNotFound() {
        DocumentChanges documentChanges = new DocumentChanges();
        mockBackEnd.enqueue(new MockResponse().setResponseCode(NOT_FOUND));

        Mono<DocumentResponse> patchDocument = documentClientCall.patchDocument("", "", "fileKey", documentChanges);
        StepVerifier.create(patchDocument).expectError(DocumentKeyNotPresentException.class).verify();
    }

    @Test
    void deleteDocumentOk() {
        Assertions.assertNull(documentClientCall.deleteDocument("fileKey"));
    }

}

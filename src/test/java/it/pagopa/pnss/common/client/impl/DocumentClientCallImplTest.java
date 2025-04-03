package it.pagopa.pnss.common.client.impl;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Document;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentInput;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentResponse;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.DocumentkeyPresentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentClientCallImplTest {

    @Mock
    private WebClient ssWebClient;

    @Mock
    private RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private ResponseSpec responseSpec;

    @InjectMocks
    private DocumentClientCallImpl documentClientCall;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @BeforeEach
    void setup(){
        ReflectionTestUtils.setField(documentClientCall, "anagraficaDocumentiClientEndpoint", "http://fakeurl/%s");
        ReflectionTestUtils.setField(documentClientCall, "anagraficaDocumentiClientEndpointPost", "http://fakeurl/post");
        ReflectionTestUtils.setField(documentClientCall, "xApiKey", "x-api-key-header");
        ReflectionTestUtils.setField(documentClientCall, "xPagopaSafestorageCxId", "x-pagopa-safestorage-cx-id-header");
    }

    @Test
    void getDocumentSuccessTest() {
        String keyFile = "testKey";
        DocumentResponse expectedResponse = new DocumentResponse();
        expectedResponse.setDocument(new Document());

        when(ssWebClient.get()).thenAnswer(invocation -> requestHeadersUriSpec); // get simulation
        when(requestHeadersUriSpec.uri(anyString())).thenAnswer(invocation -> requestHeadersSpec); // set uri simulation
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec); // http retrieve simulation
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec); // return same object to allow chaining
        when(responseSpec.bodyToMono(DocumentResponse.class)).thenReturn(Mono.just(expectedResponse)); // convert HTTP response to reactive java object Mono<DocumentResponse>

        Mono<DocumentResponse> result = documentClientCall.getDocument(keyFile);

        assertNotNull(result);
        DocumentResponse actualResponse = result.block(); // block the reactive stream to get the actual response
        assertEquals(expectedResponse, actualResponse);

        verify(ssWebClient).get();
    }

    @Test
    void getDocumentNotFoundTest() {
        String keyFile = "notFoundKey";

        when(ssWebClient.get()).thenAnswer(invocation -> requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenAnswer(invocation ->requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(DocumentResponse.class))
                .thenReturn(Mono.error(new DocumentKeyNotPresentException(keyFile)));

        assertThrows(DocumentKeyNotPresentException.class, documentClientCall.getDocument(keyFile)::block);
    }

    @Test
    void postDocumentSuccessTest() {
        DocumentInput documentInput = new DocumentInput();
        DocumentResponse expectedResponse = new DocumentResponse();
        expectedResponse.setDocument(new Document());

        when(ssWebClient.post()).thenAnswer(invocation -> requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenAnswer(invocation -> requestBodySpec);
        when(requestBodySpec.bodyValue(any(DocumentInput.class))).thenAnswer(invocation -> requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(DocumentResponse.class)).thenReturn(Mono.just(expectedResponse));

        Mono<DocumentResponse> result = documentClientCall.postDocument(documentInput);

        assertNotNull(result);
        DocumentResponse actualResponse = result.block();
        assertEquals(expectedResponse, actualResponse);

        verify(ssWebClient).post();
    }

    @Test
    void postDocumentConflictErrorTest() {
        DocumentInput documentInput = new DocumentInput();
        documentInput.setDocumentKey("existingDocumentKey");

        when(ssWebClient.post()).thenAnswer(invocation -> requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenAnswer(invocation -> requestBodySpec);
        when(requestBodySpec.bodyValue(any(DocumentInput.class))).thenAnswer(invocation -> requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(DocumentResponse.class))
                .thenReturn(Mono.error(new DocumentkeyPresentException(documentInput.getDocumentKey())));

        assertThrows(DocumentkeyPresentException.class, documentClientCall.postDocument(documentInput)::block);
    }



    @Test
    void patchDocumentSuccessTest() {
        String keyFile = "testKey";
        DocumentChanges documentChanges = new DocumentChanges();
        DocumentResponse expectedResponse = new DocumentResponse();
        expectedResponse.setDocument(new Document());

        when(ssWebClient.patch()).thenAnswer(invocation -> requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenAnswer(invocation -> requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec); // simulazione header
        when(requestBodySpec.bodyValue(any(DocumentChanges.class))).thenAnswer(invocation -> requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(DocumentResponse.class)).thenReturn(Mono.just(expectedResponse));

        Mono<DocumentResponse> result = documentClientCall.patchDocument("authId", "apiKey", keyFile, documentChanges);

        assertNotNull(result);
        DocumentResponse actualResponse = result.block();
        assertEquals(expectedResponse, actualResponse);

        verify(ssWebClient).patch();
    }

    @Test
    void deleteDocumentTest() {
        assertNull(documentClientCall.deleteDocument("testKey"));
    }
}

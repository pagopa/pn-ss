package it.pagopa.pnss.uribuilder;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.TagsClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient(timeout = "36000")
class UriBuilderRetryTagsTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UserConfigurationClientCall userConfigurationClientCall;

    @MockitoBean
    private DocumentClientCall documentClientCall;

    @MockitoBean
    private DocTypesClientCall docTypesClientCall;

    @MockitoBean
    private TagsClientCall tagsClientCall;

    @MockitoBean
    private S3Presigner s3Presigner;

    @Value("${file.upload.api.url}")
    private String urlPath;

    @Test
    void createFileWithTags_putTagsRetriesOnDocumentKeyNotFound_success() {
        DocumentType documentType = new DocumentType();
        documentType.setTipoDocumento("PN_AAR");
        documentType.setChecksum(DocumentType.ChecksumEnum.MD5);
        documentType.setTransformations(List.of());

        DocumentTypeResponse documentTypeResponse = new DocumentTypeResponse();
        documentTypeResponse.setDocType(documentType);

        when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(documentTypeResponse));

        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName("test-client");
        userConfiguration.setApiKey("test-api-key");
        userConfiguration.setCanCreate(List.of("PN_AAR"));
        userConfiguration.setCanWriteTags(true);
        userConfiguration.setDurationMinutesUpload(5);
        UserConfigurationResponse userConfigResponse = new UserConfigurationResponse();
        userConfigResponse.setUserConfiguration(userConfiguration);

        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfigResponse));

        DocumentResponseDocument documentResponseDocument = new DocumentResponseDocument();
        documentResponseDocument.setDocumentKey("PN_AAR-test-key.pdf");
        documentResponseDocument.setDocumentType(documentType);
        DocumentResponse documentResponse = new DocumentResponse();
        documentResponse.setDocument(documentResponseDocument);

        when(documentClientCall.postDocument(any())).thenReturn(Mono.just(documentResponse));

        SdkHttpRequest sdkHttpRequest = SdkHttpRequest.builder()
                .uri(URI.create("https://s3.amazonaws.com/bucket/key"))
                .method(SdkHttpMethod.PUT)
                .build();

        PresignedPutObjectRequest presignedPutObjectRequest = mock(PresignedPutObjectRequest.class);
        when(presignedPutObjectRequest.url()).thenReturn(toUrl("https://s3.amazonaws.com/bucket/PN_AAR-test-key.pdf"));
        when(presignedPutObjectRequest.httpRequest()).thenReturn(sdkHttpRequest);

        when(s3Presigner.presignPutObject(any(software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest.class))).thenReturn(presignedPutObjectRequest);

        AtomicInteger callCount = new AtomicInteger(0);
        when(tagsClientCall.putTags(any(), any())).thenReturn(
                Mono.defer(() -> {
                    if (callCount.incrementAndGet() <= 2)
                        return Mono.error(new DocumentKeyNotPresentException("PN_AAR-test-key.pdf"));
                    return Mono.just(new TagsResponse());
                })
        );

        FileCreationRequest fileCreationRequest = new FileCreationRequest();
        fileCreationRequest.setContentType("application/pdf");
        fileCreationRequest.setDocumentType("PN_AAR");
        fileCreationRequest.setStatus("preloaded");
        fileCreationRequest.setTags(Map.of("IUN", List.of("TEST-IUN-001")));

        webTestClient.post()
                .uri(urlPath)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("x-pagopa-safestorage-cx-id", "test-client")
                .header("x-api-key", "test-api-key")
                .header("x-checksum-value", "rL0Y20zC+Fzt72VPzMSk2A==")
                .header("x-amzn-trace-id", "trace-123")
                .bodyValue(fileCreationRequest)
                .exchange()
                .expectStatus().isOk();

        assertThat(callCount.get()).isEqualTo(3);
    }

    private java.net.URL toUrl(String url) {
        try {
            return URI.create(url).toURL();
        } catch (java.net.MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}

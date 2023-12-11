package it.pagopa.pnss.uribuilder;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static it.pagopa.pnss.common.DocTypesConstant.*;
import static it.pagopa.pnss.common.UserConfigurationConstant.*;
import static it.pagopa.pnss.common.constant.Constant.*;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;


@SpringBootTestWebEnv
@AutoConfigureWebTestClient(timeout = "36000")
@CustomLog
class UriBuilderUploadTest {
    @Autowired
    private WebTestClient webClient;
    @Value("${header.x-api-key:#{null}}")
    private String xApiKey;
    @Value("${header.x-pagopa-safestorage-cx-id:#{null}}")
    private String X_PAGOPA_SAFESTORAGE_CX_ID;
    @Value("${queryParam.presignedUrl.traceId:#{null}}")
    private String queryParamPresignedUrlTraceId;
    @Value("${header.x-checksum-value:#{null}}")
    private String headerChecksumValue;
    @Value("${header.presignedUrl.checksum-md5:#{null}}")
    private String headerChecksumMd5;
    @Value("${header.presignedUrl.checksum-sha256:#{null}}")
    private String headerChecksumSha256;
    @Value("${file.upload.api.url}")
    private String urlPath;
    private static final String xChecksumValue = "checkSumValue";
    private static final String X_QUERY_PARAM_URL_VALUE= "queryParamPresignedUrlTraceId_value";
    private static final DocumentResponse DOCUMENT_RESPONSE = new DocumentResponse().document(new Document().documentKey("documentKey").documentType(new DocumentType().checksum(DocumentType.ChecksumEnum.MD5)));
    private WebTestClient.ResponseSpec fileUploadTestCall(FileCreationRequest fileCreationRequest, String clientId, String apiKey, String traceId) {

        WebTestClient.RequestHeadersSpec requestHeadersSpec = this.webClient.post()
                .uri(urlPath)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fileCreationRequest)
                .header(X_PAGOPA_SAFESTORAGE_CX_ID, clientId)
                .header(headerChecksumValue, xChecksumValue)
                .header(xApiKey, apiKey);

        if (!StringUtils.isBlank(traceId))
            requestHeadersSpec.header(queryParamPresignedUrlTraceId, traceId);

        return requestHeadersSpec.exchange();
    }
    @Test
    void testUploadMissingTraceIdHeader()
    {
        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType(APPLICATION_PDF_VALUE);
        fcr.setDocumentType(PN_AAR);
        fcr.setStatus(PRELOADED);
        fileUploadTestCall(fcr, PN_TEST, PN_TEST_API_KEY, null).expectStatus().isBadRequest();
    }

    @Test
    void testUploadMissingContentType() {
        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setDocumentType(PN_AAR);
        fcr.setStatus("VALUE_FAULT");
        fileUploadTestCall(fcr, PN_TEST, PN_TEST_API_KEY, X_QUERY_PARAM_URL_VALUE).expectStatus().isBadRequest();
    }

    @Test
    void testUploadMissingDocumentType() {
        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType(APPLICATION_PDF_VALUE);
        fcr.setStatus("");
        fileUploadTestCall(fcr, PN_TEST, PN_TEST_API_KEY, X_QUERY_PARAM_URL_VALUE).expectStatus().isBadRequest();
    }

    @Test
    void testUploadBadContentType() {
        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType("application/badContentType");
        fcr.setDocumentType(PN_AAR);
        fcr.setStatus("");
        fileUploadTestCall(fcr, PN_TEST, PN_TEST_API_KEY, X_QUERY_PARAM_URL_VALUE).expectStatus().isOk();
    }

//    @Test
//    void testIdClienteNonTrovatoUpload() {
//        //Mancata gestione della IdClientNotFoundException. Ritorna 500 invece di 404.
//        var fcr = new FileCreationRequest().contentType(IMAGE_TIFF_VALUE).documentType(PN_AAR).status("");
//        callRequestHeadersSpec(fcr)
//                .header(queryParamPresignedUrlTraceId, X_QUERY_PARAM_URL_VALUE)
//                .header(headerChecksumValue, xChecksumValue)
//                .header(X_PAGOPA_SAFESTORAGE_CX_ID, "BAD_CLIENT_ID")
//                .exchange().expectStatus()
//                .isNotFound();
//    }

    @ParameterizedTest
    @ValueSource(strings = {PN_NOTIFICATION_ATTACHMENTS, PN_LOG_EXTRACTOR_RESULT, PN_NONE_CHECKSUM})
    void testUrlGenerato(String documentType) {

        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType(APPLICATION_PDF_VALUE);
        fcr.setDocumentType(documentType);
        fcr.setStatus("");

        fileUploadTestCall(fcr, PN_TEST, PN_TEST_API_KEY, X_QUERY_PARAM_URL_VALUE)
                .expectStatus()
                .isOk()
                .expectBody(FileCreationResponse.class)
                .value(hasProperty("uploadUrl", notNullValue()));
    }

    @Test
    void testUploadUnauthorizedClientId() {
        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType(IMAGE_TIFF_VALUE);
        fcr.setDocumentType(PN_AAR);
        fcr.setStatus("");
        fileUploadTestCall(fcr, PN_DELIVERY, PN_DELIVERY_API_KEY, X_QUERY_PARAM_URL_VALUE).expectStatus().isForbidden();
    }

}

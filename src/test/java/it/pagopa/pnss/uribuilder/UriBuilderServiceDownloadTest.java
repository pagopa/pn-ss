package it.pagopa.pnss.uribuilder;

import it.pagopa.pn.template.internal.rest.v1.dto.*;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static it.pagopa.pnss.common.Constant.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Slf4j
class UriBuilderServiceDownloadTest {

    @Value("${header.x-api-key:#{null}}")
    private String xApiKey;
    @Value("${header.x-pagopa-safestorage-cx-id:#{null}}")
    private String X_PAGOPA_SAFESTORAGE_CX_ID;

    private static final String X_API_KEY_VALUE = "apiKey_value";
    private static final String X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE = "CLIENT_ID_123";

    private static final UserConfigurationResponse USER_CONFIGURATION_RESPONSE =
            new UserConfigurationResponse().userConfiguration(new UserConfiguration().apiKey(X_API_KEY_VALUE));

    @Value("${file.download.api.url}")
    public String urlDownload;

    @Autowired
    private WebTestClient webClient;

    @MockBean
    UserConfigurationClientCall userConfigurationClientCall;

    @MockBean
    DocumentClientCall documentClientCall;
    
    private static final String CHECKSUM = "91375e9e5a9510087606894437a6a382fa5bc74950f932e2b85a788303cf5ba0";

    private WebTestClient.ResponseSpec fileDownloadTestCall(String requestIdx, Boolean metadataOnly) {
        this.webClient.mutate().responseTimeout(Duration.ofMillis(30000)).build();
        return this.webClient.get()
                             .uri(uriBuilder -> uriBuilder.path(urlDownload).queryParam("metadataOnly", metadataOnly)
                                                          //... building a URI
                                                          .build(requestIdx))
                             .header(X_PAGOPA_SAFESTORAGE_CX_ID, X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE)
                             .header(xApiKey, X_API_KEY_VALUE)
                             .header(HttpHeaders.ACCEPT, "application/json")
                             .attribute("metadataOnly", false)
                             .exchange();


    }

    @BeforeEach
    private void createUserConfiguration() {

        log.info("createUserConfiguration() : START");

        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE);
        userConfiguration.setApiKey(X_API_KEY_VALUE);

        UserConfigurationResponse userConfig = new UserConfigurationResponse();
        userConfig.setUserConfiguration(userConfiguration);

        Mono<UserConfigurationResponse> userConfigurationResponse = Mono.just(userConfig);
        Mockito.doReturn(userConfigurationResponse).when(userConfigurationClientCall).getUser(Mockito.any());

        log.info("createUserConfiguration() : END");
    }

    private String getDownloadFileEndpoint() {
        return urlDownload;
    }


    @Test
    void testUrlGenerato() {

        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));

        String docId = "1111-aaaa";
        mockUserConfiguration(List.of(PN_AAR));

        DocumentInput d = new DocumentInput();
        d.setDocumentType(PN_AAR);
        d.setDocumentState(AVAILABLE);
        d.setCheckSum(CHECKSUM);

        mockGetDocument(d, docId);

        fileDownloadTestCall(docId, true).expectStatus().isOk();
    }

    @Test
    void testUrlGeneratoConMetaDataTrue() {
        String docId = "1111-aaaa";

        mockUserConfiguration(List.of(PN_AAR));


        DocumentInput d = new DocumentInput();
        d.setDocumentType(PN_AAR);
        d.setDocumentState(AVAILABLE);
        d.setCheckSum(CHECKSUM);

        mockGetDocument(d, docId);

        fileDownloadTestCall(docId, true).expectStatus().isOk();
    }

    @Test
    void testUrlGeneratoConMetaDataNull() {
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));

        String docId = "1111-aaaa";

        mockUserConfiguration(List.of(PN_AAR));

        DocumentInput d = new DocumentInput();
        d.setDocumentType(PN_AAR);
        d.setDocumentState(AVAILABLE);
        d.setCheckSum(CHECKSUM);

        mockGetDocument(d, docId);

        fileDownloadTestCall(docId, true).expectStatus().isOk();
    }


//    @Test
//    void testFileTrovatoBasketHot(){
//        String docId = "1111-aaaa";
//
//
//
//
//        DocumentInput d = new DocumentInput();
//        d.setDocumentType(PN_AAR);
//        d.setDocumentState(AVAILABLE);
//        d.setCheckSum(DocumentInput.CheckSumEnum.SHA256);
//
//        mockGetDocument(d, docId);
//
//
//        //Mockito.doReturn(fdr).when(service).createUriForDownloadFile(Mockito.any(), Mockito.any());
//        fileDownloadTestCall( docId,true).expectStatus()
//                .isOk().expectBody(FileDownloadResponse.class).value(response ->{
//                    Assertions.assertThat(!response.getChecksum().isEmpty());
//                    Assertions.assertThat(StringUtils.isNotEmpty(response.getDownload().getUrl()));
//
//                });
//    }

//    @Test
//    void testFileTrovatoBasketCold(){
//        String docId = "1111-aaaa";
//        mockUserConfiguration(List.of(PN_AAR));
//
//
//        DocumentInput d = new DocumentInput();
//        d.setDocumentType(PN_AAR);
//        d.setDocumentState(AVAILABLE);
//        d.setCheckSum(DocumentInput.CheckSumEnum.SHA256);
//        mockGetDocument(d, docId);
//        //Mockito.doReturn(fdr).when(service).createUriForDownloadFile(Mockito.any(), Mockito.any());
//        fileDownloadTestCall( docId,true).expectStatus()
//                .isOk().expectBody(FileDownloadResponse.class).value(response ->{
//                    Assertions.assertThat(!response.getChecksum().isEmpty());
//                    //TODO rimettere
////                    Assertions.assertThat(!response.getDownload().getRetryAfter().equals(MAX_RECOVER_COLD));
//
//                });
//    }

    @Test
    void testFileNonTrovato() {

        String docId = "1111-aaaa";
        DocumentInput d = new DocumentInput();
        d.setDocumentType(PN_AAR);
        mockUserConfiguration(List.of(PN_AAR));
        mockGetDocument(d, docId);
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));
        Mockito.when(documentClientCall.getdocument(Mockito.any())).thenReturn(Mono.error(new DocumentKeyNotPresentException("keyFile")));
        fileDownloadTestCall(docId, null).expectStatus().isNotFound();

    }

    @Test
    void testIdClienteNonTrovatoDownload() {

        Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User Not Found : "))
               .when(userConfigurationClientCall)
               .getUser(Mockito.any());
        String docId = "1111-aaaa";
        fileDownloadTestCall(docId, null).expectStatus().isNotFound();

    }

    @Test
    void testIdClienteNoPermessiDownload() {
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));

        String docId = "1111-aaaa";

        mockUserConfiguration(List.of(PN_NOTIFICATION_ATTACHMENTS));

        DocumentInput d = new DocumentInput();
        d.setDocumentType(PN_AAR);
        d.setDocumentState(technicalStatus_available);

        mockGetDocument(d, docId);
        fileDownloadTestCall(docId, false).expectStatus().isForbidden();
    }

    @Test
    void testDocumentNotInStatusAvailable() {
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));

        String docId = "1111-aaaa";

        mockUserConfiguration(List.of(PN_NOTIFICATION_ATTACHMENTS));

        DocumentInput d = new DocumentInput();
        d.setDocumentType(PN_AAR);
        d.setDocumentState(technicalStatus_attached);

        mockGetDocument(d, docId);
        fileDownloadTestCall(docId, false).expectStatus().isBadRequest();
    }

    private void mockGetDocument(DocumentInput d, String docId) {
        DocumentResponse documentResponse = new DocumentResponse();
        Document doc = new Document();
        DocumentType type = new DocumentType();
        type.setTipoDocumento(d.getDocumentType());
        doc.setDocumentType(type);
        doc.setDocumentState(d.getDocumentState());
        documentResponse.setDocument(doc);
        Mono<DocumentResponse> docRespEntity = Mono.just(documentResponse);
        Mockito.doReturn(docRespEntity).when(documentClientCall).getdocument(docId);
    }

    private void mockUserConfiguration(List<String> permessi) {
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(new UserConfigurationResponse().userConfiguration(new UserConfiguration().canRead(
                permessi).apiKey(X_API_KEY_VALUE))));
    }
}

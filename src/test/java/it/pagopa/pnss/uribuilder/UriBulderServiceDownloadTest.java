package it.pagopa.pnss.uribuilder;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentResponse;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfiguration;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfigurationResponse;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
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

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Slf4j
public class UriBulderServiceDownloadTest {

    public static final String X_PAGOPA_SAFESTORAGE_CX_ID = "x-pagopa-safestorage-cx-id";

    @Value("${file.download.api.url}")
    public  String urlDownload;

    @Autowired
    private WebTestClient webClient;

    @MockBean
    UserConfigurationClientCall userConfigurationClientCall;

    @MockBean
    DocumentClientCall documentClientCall;
    private WebTestClient.ResponseSpec fileDownloadTestCall(
                          String requestIdx) {
        this.webClient.mutate()
                .responseTimeout(Duration.ofMillis(30000))
                .build();
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(urlDownload)
                        //... building a URI
                        .build(requestIdx))
                .header(X_PAGOPA_SAFESTORAGE_CX_ID, "CLIENT_ID_123")
                .header(HttpHeaders.ACCEPT, "application/json")
                .attribute("metadataOnly",false)
                .exchange();


    }

    private String getDownloadFileEndpoint() {
        return urlDownload;
    }


    @Test
    public void testUrlGenerato(){

        String docId = "1111-aaaa";

        mockUserConfiguration(List.of(PN_AAR));

        Document d = new Document();
        d.setDocumentType(Document.DocumentTypeEnum.AAR);
        d.setDocumentState(Document.DocumentStateEnum.AVAILABLE);
        d.setCheckSum(Document.CheckSumEnum.SHA256);



        mockGetDocument(d, docId);

        fileDownloadTestCall(docId).expectStatus()
                .isOk();
    }



    @Test
    public void testFileTrovatoBasketHot(){
        String docId = "1111-aaaa";

        mockUserConfiguration(List.of(PN_AAR));

        Document d = new Document();
        d.setDocumentType(Document.DocumentTypeEnum.AAR);
        d.setDocumentState(Document.DocumentStateEnum.AVAILABLE);
        d.setCheckSum(Document.CheckSumEnum.SHA256);

        mockGetDocument(d, docId);


        //Mockito.doReturn(fdr).when(service).createUriForDownloadFile(Mockito.any(), Mockito.any());
        fileDownloadTestCall( docId).expectStatus()
                .isOk().expectBody(FileDownloadResponse.class).value(response ->{
                    Assertions.assertThat(!response.getChecksum().isEmpty());
                    Assertions.assertThat(StringUtils.isNotEmpty(response.getDownload().getUrl()));

                });
    }

    @Test
    public void testFileTrovatoBasketCold(){
        String docId = "1111-aaaa";
        mockUserConfiguration(List.of(PN_AAR));


        Document d = new Document();
        d.setDocumentType(Document.DocumentTypeEnum.AAR);
        d.setDocumentState(Document.DocumentStateEnum.FREEZED);
        d.setCheckSum(Document.CheckSumEnum.SHA256);
        mockGetDocument(d, docId);

        //Mockito.doReturn(fdr).when(service).createUriForDownloadFile(Mockito.any(), Mockito.any());
        fileDownloadTestCall( docId).expectStatus()
                .isOk().expectBody(FileDownloadResponse.class).value(response ->{
                    Assertions.assertThat(!response.getChecksum().isEmpty());
                    Assertions.assertThat(!response.getDownload().getRetryAfter().equals(MAX_RECOVER_COLD));

                });
    }

    @Test
    public void testFileNonTrovato(){

        String docId = "1111-aaaa";

        mockUserConfiguration(List.of(PN_AAR));

        mockGetDocument(null, docId);
        Mockito.when(documentClientCall.getdocument(Mockito.any())).thenReturn(Mono.error(new DocumentKeyNotPresentException("keyFile")));
        fileDownloadTestCall( docId).expectStatus()
                .isNotFound();

    }

    @Test
    public void testIdClienteNonTrovatoDownload(){
        Mockito.doThrow(new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User Not Found : ")).when(userConfigurationClientCall).getUser(Mockito.any());
        String docId = "1111-aaaa";
        fileDownloadTestCall( docId).expectStatus()
                .isNotFound();

    }

    @Test
    public void testIdClienteNoPermessiDownload(){
        String docId = "1111-aaaa";

        mockUserConfiguration(List.of(PN_NOTIFICATION_ATTACHMENTS));

        Document d = new Document();
        d.setDocumentType(Document.DocumentTypeEnum.AAR);

        mockGetDocument(d, docId);

        fileDownloadTestCall(docId).expectStatus()
                .isBadRequest();
    }


    private void mockGetDocument(Document d, String docId) {
        DocumentResponse documentResponse= new DocumentResponse();
        documentResponse.setDocument(d);
        Mono<DocumentResponse> docRespEntity = Mono.just(documentResponse);
        Mockito.doReturn(docRespEntity).when(documentClientCall).getdocument(docId);
    }

    private void mockUserConfiguration(List<String> permessi) {
        UserConfigurationResponse userConfig = new UserConfigurationResponse();
        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setCanRead(permessi);
        userConfig.setUserConfiguration(userConfiguration);
        Mono<UserConfigurationResponse> userConfigurationEntity = Mono.just(userConfig)  ;

        Mockito.doReturn(userConfigurationEntity).when(userConfigurationClientCall).getUser(Mockito.any());
    }

}

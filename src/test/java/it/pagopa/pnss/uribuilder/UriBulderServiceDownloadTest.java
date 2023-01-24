package it.pagopa.pnss.uribuilder;

import it.pagopa.pn.template.rest.v1.dto.FileDownloadInfo;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pnss.uriBuilder.rest.FileDownloadApiController;
import it.pagopa.pnss.uriBuilder.service.UriBuilderService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@WebFluxTest(FileDownloadApiController.class)
public class UriBulderServiceDownloadTest {

    public static final String X_PAGOPA_SAFESTORAGE_CX_ID = "x-pagopa-safestorage-cx-id";

    public static final String urlDownload = "/safe-storage/v1/files/{fileKey}";

    @Autowired
    private WebTestClient webClient;

    @MockBean
    UriBuilderService service;

    private WebTestClient.ResponseSpec fileDownloadTestCall(
                                                          String requestIdx) {

        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(urlDownload)
                        //... building a URI
                        .build("dfsgdsg"))
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
        Mockito.doReturn(new FileDownloadResponse()).when(service).createUriForDownloadFile(Mockito.any());
        fileDownloadTestCall("4444").expectStatus()
                .isOk();
    }

    @Test
    public void testFileTrovatoBasketHot(){
        FileDownloadResponse fdr = new FileDownloadResponse();
        fdr.setChecksum("");
        FileDownloadInfo download = new FileDownloadInfo();
        download.setUrl("url_generato");
        fdr.setDownload(download);
        Mockito.doReturn(fdr).when(service).createUriForDownloadFile(Mockito.any());
        fileDownloadTestCall( X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus()
                .isOk().expectBody(FileDownloadResponse.class).value(response ->{
                    Assertions.assertThat(!response.getChecksum().isEmpty());
                    Assertions.assertThat(!response.getDownload().getUrl().equals("url_generato"));

                });
    }

    @Test
    public void testFileTrovatoBasketCold(){
        FileDownloadResponse fdr = new FileDownloadResponse();
        fdr.setChecksum("");
        FileDownloadInfo download = new FileDownloadInfo();
        download.setRetryAfter(UriBuilderService.MAX_RECOVER_COLD);
        fdr.setDownload(download);
        Mockito.doReturn(fdr).when(service).createUriForDownloadFile(Mockito.any());
        fileDownloadTestCall( X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus()
                .isOk().expectBody(FileDownloadResponse.class).value(response ->{
                    Assertions.assertThat(!response.getChecksum().isEmpty());
                    Assertions.assertThat(!response.getDownload().getRetryAfter().equals(UriBuilderService.MAX_RECOVER_COLD));

                });
    }

    @Test
    public void testFileNotrTrovato(){
        fileDownloadTestCall( X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus()
                .isNotFound();
    }


    @Test
    public void testInternalServerError(){
        Mockito.doThrow(new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "ResponseStatusException -> Message  "+" Amazon S3 couldn't be contacted for a response, or the client couldn't parse the response from Amazon S3. ")
                ).when(service).createUriForDownloadFile(Mockito.any());
        fileDownloadTestCall( X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus()
                .is5xxServerError();
    }




}

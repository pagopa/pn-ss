package it.pagopa.pnss.uribuilder;

import it.pagopa.pn.template.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;
import it.pagopa.pnss.uriBuilder.rest.FileUploadApiController;
import it.pagopa.pnss.uriBuilder.service.UriBuilderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;

import static it.pagopa.pnss.common.Constant.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ExtendWith(SpringExtension.class)
@WebFluxTest(FileUploadApiController.class)
public class UriBulderUploadTest {



    public static final String X_PAGOPA_SAFESTORAGE_CX_ID = "x-pagopa-safestorage-cx-id";

    @Value("${file.upload.api.url}")
    String urlPath;
    @Autowired
    private WebTestClient webClient;

    @MockBean
    UriBuilderService service;
    private static final String ID_CLIENT_HEADER = "x-pagopa-safestorage-cx-id";


    private WebTestClient.ResponseSpec fileUploadTestCall(BodyInserter<FileCreationRequest, ReactiveHttpOutputMessage> bodyInserter,
                                                          String requestIdx) {
        return this.webClient.post()
                .uri(getUploadFileEndpoint(requestIdx))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(bodyInserter)
                .header(X_PAGOPA_SAFESTORAGE_CX_ID, "CLIENT_ID_123")
                .exchange();
    }

    private String getUploadFileEndpoint(String requestIdx) {
        return urlPath;
    }




    @Test
    public void testUrlGenStatusPre() throws Exception {
        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType(IMAGE_TIFF);
        fcr.setDocumentType("PN_NOTIFICATION_ATTACHMENTS");
        fcr.setStatus(PRELOADED);
        FileCreationResponse fcresp = new FileCreationResponse();
        fcresp.setUploadUrl("http://host:9090/urlFile");

        Mockito.doReturn(fcresp).when(service).createUriForUploadFile(Mockito.any(), Mockito.any(),Mockito.any(),Mockito.any());
        fileUploadTestCall(BodyInserters.fromValue(fcr),X_PAGOPA_SAFESTORAGE_CX_ID) .expectStatus()
                .isOk();

    }

    @Test
    public void testStatoNonConsentito_PN_NOTIFICATION_ATTACHMENTS(){
        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType(IMAGE_TIFF);
        fcr.setDocumentType("PN_NOTIFICATION_ATTACHMENTS");
        fcr.setStatus("ATTACHED");
        fileUploadTestCall(BodyInserters.fromValue(fcr),X_PAGOPA_SAFESTORAGE_CX_ID) .expectStatus()
                .isBadRequest();
    }

    @Test
    public void testUrlGenerato() throws InterruptedException {
        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType(IMAGE_TIFF);
        fcr.setDocumentType("PN_AAR");
        fcr.setStatus("");
        FileCreationResponse fcresp = new FileCreationResponse();
        fcresp.setUploadUrl("http://host:9090/urlFile");
        Mockito.doReturn(fcresp).when(service).createUriForUploadFile(Mockito.any(), Mockito.any(),Mockito.any(),Mockito.any());
        fileUploadTestCall(BodyInserters.fromValue(fcr),X_PAGOPA_SAFESTORAGE_CX_ID) .expectStatus()
                .isOk();

    }

    @Test
    public void testStatoNonConsentito_PN_AAR(){
        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType(IMAGE_TIFF);
        fcr.setDocumentType("PN_AAR");
        fcr.setStatus(PRELOADED);
        fileUploadTestCall(BodyInserters.fromValue(fcr),X_PAGOPA_SAFESTORAGE_CX_ID) .expectStatus()
                .isBadRequest();
    }



    @Test
    public void testErroreInserimentoContentType(){
        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType("VALUE_FAULT");
        fcr.setDocumentType("PN_AAR");
        fcr.setStatus(PRELOADED);
         fileUploadTestCall(BodyInserters.fromValue(fcr),X_PAGOPA_SAFESTORAGE_CX_ID) .expectStatus()
                .isBadRequest();
    }

    @Test
    public void testErroreInserimentoDocumentType(){
        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType(IMAGE_TIFF);
        fcr.setDocumentType("VALUE_FAULT");
        fcr.setStatus(PRELOADED);
         fileUploadTestCall(BodyInserters.fromValue(fcr),X_PAGOPA_SAFESTORAGE_CX_ID) .expectStatus()
                .isBadRequest();
    }

    @Test
    public void testErroreInserimentoStatus(){
        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType(IMAGE_TIFF);
        fcr.setDocumentType(PN_AAR);
        fcr.setStatus("VALUE_FAULT");

        fileUploadTestCall(BodyInserters.fromValue(fcr),X_PAGOPA_SAFESTORAGE_CX_ID) .expectStatus()
                .isBadRequest();
    }

    @Test
    public void testContetTypeParamObbligatorio(){
        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setDocumentType("PN_AAR");
        fcr.setStatus("VALUE_FAULT");

        fileUploadTestCall(BodyInserters.fromValue(fcr),X_PAGOPA_SAFESTORAGE_CX_ID) .expectStatus()
                .isBadRequest();
    }

    @Test
    public void testDocumentTypeParamObbligatorio(){
        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setDocumentType("PN_AAR");
        fcr.setStatus("VALUE_FAULT");

        fileUploadTestCall(BodyInserters.fromValue(fcr),X_PAGOPA_SAFESTORAGE_CX_ID) .expectStatus()
                .isBadRequest();
    }

    public void testIdClienteNonTrovatoUpload(){

    }

    public void testIdClienteNoPermessiUpload(){

    }

}

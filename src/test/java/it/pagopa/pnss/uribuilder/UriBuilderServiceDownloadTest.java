package it.pagopa.pnss.uribuilder;

import it.pagopa.pn.template.internal.rest.v1.dto.*;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pnss.common.DocTypesConstant;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.S3BucketException;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.RestoreObjectRequest;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.List;

import static it.pagopa.pnss.common.constant.Constant.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Slf4j
class UriBuilderServiceDownloadTest {

    @Value("${header.x-api-key:#{null}}")
    private String xApiKey;
    @Value("${header.x-pagopa-safestorage-cx-id:#{null}}")
    private String X_PAGOPA_SAFESTORAGE_CX_ID;
    @Value("${queryParam.presignedUrl.traceId:#{null}}")
    private String queryParamPresignedUrlTraceId;

    @Value("${max.restore.time.cold}")
    BigDecimal maxRestoreTimeCold;

    private static final String X_API_KEY_VALUE = "apiKey_value";
    private static final String X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE = "CLIENT_ID_123";
    private static final String X_QUERY_PARAM_URL_VALUE= "queryParamPresignedUrlTraceId_value";

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

    @MockBean
    DocTypesClientCall docTypesClientCall;
    
    @SpyBean
    UriBuilderService uriBuilderService;

    @Value("${test.aws.s3.endpoint:#{null}}")
    String testAwsS3Endpoint;

    @Autowired
    BucketName bucketName;
    private static final String CHECKSUM = "91375e9e5a9510087606894437a6a382fa5bc74950f932e2b85a788303cf5ba0";

    private WebTestClient.ResponseSpec fileDownloadTestCall(String requestIdx, Boolean metadataOnly) {
        this.webClient.mutate().responseTimeout(Duration.ofMillis(30000)).build();
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path(urlDownload).queryParam("metadataOnly", metadataOnly)
                        //... building a URI
                        .build(requestIdx))
                .header(X_PAGOPA_SAFESTORAGE_CX_ID, X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE)
                .header(xApiKey, X_API_KEY_VALUE)
                .header(queryParamPresignedUrlTraceId, X_QUERY_PARAM_URL_VALUE)
                .header(HttpHeaders.ACCEPT, "application/json")
                .attribute("metadataOnly", metadataOnly)
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
        doReturn(userConfigurationResponse).when(userConfigurationClientCall).getUser(Mockito.any());

        log.info("createUserConfiguration() : END");
    }

    private String getDownloadFileEndpoint() {
        return urlDownload;
    }


    @Test
    void testUrlGenerato() {

        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));

        String docId = "1111-aaaa";
        mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));

        DocumentInput d = new DocumentInput();
        d.setDocumentType(DocTypesConstant.PN_AAR);
        d.setDocumentState(AVAILABLE);
        d.setCheckSum(CHECKSUM);

        mockGetDocument(d, docId);

        when(docTypesClientCall.getdocTypes(DocTypesConstant.PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));

        fileDownloadTestCall(docId, true).expectStatus().isOk();
    }

    @Test
    void testUrlGeneratoConMetaDataTrue() {
        String docId = "1111-aaaa";

        mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));


        DocumentInput d = new DocumentInput();
        d.setDocumentType(DocTypesConstant.PN_AAR);
        d.setDocumentState(AVAILABLE);
        d.setCheckSum(CHECKSUM);

        mockGetDocument(d, docId);

        when(docTypesClientCall.getdocTypes(DocTypesConstant.PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));

        fileDownloadTestCall(docId, true).expectStatus().isOk();
    }

    @Test
    void testUrlGeneratoConMetaDataNull() {
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));

        String docId = "1111-aaaa";

        mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));

        DocumentInput d = new DocumentInput();
        d.setDocumentType(DocTypesConstant.PN_AAR);
        d.setDocumentState(AVAILABLE);
        d.setCheckSum(CHECKSUM);

        mockGetDocument(d, docId);

        when(docTypesClientCall.getdocTypes(DocTypesConstant.PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));

        fileDownloadTestCall(docId, true).expectStatus().isOk();
    }


    @Test
    void testFileTrovatoBasketHot(){
        String docId = "1111-aaaa";
        mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));

        DocumentInput d = new DocumentInput();
        d.setDocumentType(DocTypesConstant.PN_AAR);
        d.setDocumentState(AVAILABLE);
        d.setCheckSum("");
        mockGetDocument(d, docId);
        //Mockito.doReturn(fdr).when(service).createUriForDownloadFile(Mockito.any(), Mockito.any());
        when(docTypesClientCall.getdocTypes(DocTypesConstant.PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));


        fileDownloadTestCall( docId,false).expectStatus()
                .isOk().expectBody(FileDownloadResponse.class).value(response ->{
                    Assertions.assertThat(StringUtils.isNotEmpty(response.getDownload().getUrl()));
                });
    }


    @Test
    void testUrlStatusDeleted() {

        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));

        String docId = "1111-aaaa";
        mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));

        DocumentInput d = new DocumentInput();
        d.setDocumentType(DocTypesConstant.PN_AAR);
        d.setDocumentState(DELETED);
        d.setCheckSum(CHECKSUM);

        mockGetDocument(d, docId);

        when(docTypesClientCall.getdocTypes(DocTypesConstant.PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));

        fileDownloadTestCall(docId, true).expectStatus().isEqualTo(HttpStatus.GONE);
    }
    
    @Test
    void testDocumentMissingFromBucket() {

        doThrow(new S3BucketException.NoSuchKeyException("")).when(uriBuilderService).createFileDownloadInfo(any(), any(), any(), anyBoolean());

        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));
    	
    	String docId = "1111-aaaa"; mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));
    	DocumentInput d = new DocumentInput(); d.setDocumentType(DocTypesConstant.PN_AAR);
    	d.setDocumentState(TECHNICAL_STATUS_BOOKED);
    	
    	when(docTypesClientCall.getdocTypes(DocTypesConstant.PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));
    	
    	mockGetDocument(d, docId);
    	fileDownloadTestCall(docId, false).expectStatus().isEqualTo(HttpStatus.GONE);
    }

    @Test void testDocumentMissingFromBucketFreezed() {

        doThrow(new S3BucketException.NoSuchKeyException("")).when(uriBuilderService).createFileDownloadInfo(any(), any(), any(), anyBoolean());

        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));

        String docId = "1111-aaaa"; mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));
        DocumentInput d = new DocumentInput(); d.setDocumentType(DocTypesConstant.PN_AAR);
        d.setDocumentState(TECHNICAL_STATUS_FREEZED);

        when(docTypesClientCall.getdocTypes(DocTypesConstant.PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));

        mockGetDocument(d, docId);
        fileDownloadTestCall(docId, false).expectStatus().isEqualTo(HttpStatus.GONE);
    }

   /* @Test
    void testFileTrovatoBasketCold() {
        String docId = "1111-aaaa";
        mockUserConfiguration(List.of(PN_AAR));


        DocumentInput d = new DocumentInput();
        d.setDocumentType(PN_AAR);
        d.setDocumentState(FREEZED);
        d.setDocumentLogicalState(SAVED);
        d.setCheckSum("" );
        mockGetDocument(d, docId);
        addFileToBucket(docId);
        //Mockito.doReturn(fdr).when(service).createUriForDownloadFile(Mockito.any(), Mockito.any());
        when(docTypesClientCall.getdocTypes(PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));

        fileDownloadTestCall( docId,false).expectStatus()
                .isOk().expectBody(FileDownloadResponse.class).value(response ->{
                    //Assertions.assertThat(!response.getChecksum().isEmpty());
                    //TODO rimettere
                    Assertions.assertThat(!response.getDownload().getRetryAfter().equals(maxRestoreTimeCold));

                });
    }*/

    @Test
    void testFileNonTrovato() {

        String docId = "1111-aaaa";
        DocumentInput d = new DocumentInput();
        d.setDocumentType(DocTypesConstant.PN_AAR);
        mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));
        mockGetDocument(d, docId);
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));
        Mockito.when(documentClientCall.getDocument(Mockito.any())).thenReturn(Mono.error(new DocumentKeyNotPresentException("keyFile")));
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

        mockUserConfiguration(List.of(DocTypesConstant.PN_NOTIFICATION_ATTACHMENTS));

        DocumentInput d = new DocumentInput();
        d.setDocumentType(DocTypesConstant.PN_AAR);
        d.setDocumentState(TECHNICAL_STATUS_AVAILABLE);

        mockGetDocument(d, docId);
        fileDownloadTestCall(docId, false).expectStatus().isForbidden();
    }

    @Test
    void testDocumentNotInStatusAvailable() {
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));

        String docId = "1111-aaaa";

        mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));

        DocumentInput d = new DocumentInput();
        d.setDocumentType(DocTypesConstant.PN_AAR);
        d.setDocumentState(TECHNICAL_STATUS_BOOKED);

        when(docTypesClientCall.getdocTypes(DocTypesConstant.PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));

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
        doc.setDocumentLogicalState(d.getDocumentLogicalState());
        documentResponse.setDocument(doc);
        Mono<DocumentResponse> docRespEntity = Mono.just(documentResponse);
        doReturn(docRespEntity).when(documentClientCall).getDocument(docId);
    }

    private void mockUserConfiguration(List<String> permessi) {
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(new UserConfigurationResponse().userConfiguration(new UserConfiguration().canRead(
                permessi).apiKey(X_API_KEY_VALUE))));
    }
    private void addFileToBucket(String fileName) {
        S3ClientBuilder client = S3Client.builder();
        client.endpointOverride(URI.create(testAwsS3Endpoint));
        S3Client s3Client = client.build();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName.ssHotName())
                .storageClass(StorageClass.GLACIER)
                .key(fileName).build();

        s3Client.putObject(request, RequestBody.fromBytes(readPdfDocoument()));
    }

    private byte[] readPdfDocoument() {
        byte[] byteArray=null;
        try {
            InputStream is =  getClass().getResourceAsStream("/PDF_PROVA.pdf");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            byteArray = buffer.toByteArray();

        } catch (FileNotFoundException e) {
            System.out.println("File Not found"+e);
        } catch (IOException e) {
            System.out.println("IO Ex"+e);
        }
        return byteArray;

    }

}

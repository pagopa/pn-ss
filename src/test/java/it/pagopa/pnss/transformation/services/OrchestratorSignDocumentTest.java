package it.pagopa.pnss.transformation.services;

import static it.pagopa.pnss.common.Constant.APPLICATION_PDF;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentResponse;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfiguration;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfigurationStatuses;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pn.template.rest.v1.dto.StorageConfiguration;
import it.pagopa.pnss.common.client.ConfigurationApiCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.S3BucketException;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.model.Detail;
import it.pagopa.pnss.transformation.model.Oggetto;
import it.pagopa.pnss.transformation.model.S3ObjectCreated;
import it.pagopa.pnss.transformation.service.DownloadObjectService;
import it.pagopa.pnss.transformation.service.OrchestratorSignDocument;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
public class OrchestratorSignDocumentTest {
    @MockBean
    DocumentClientCall documentClientCall;
    @MockBean
    ConfigurationApiCall configurationApiCall;


    @Autowired
    OrchestratorSignDocument service;
    @Autowired
    BucketName bucketName;

    @Autowired
    DownloadObjectService downloadObjectService;
    @Value("${test.aws.s3.endpoint:#{null}}")
    String testAwsS3Endpoint;
    
    private static DocumentTypesConfigurations documentTypesConfigurationsReponse;
    
    @BeforeEach
    private void createDocumentsConfigResponse() {
    	
    	List<String> listAllowedStatusTransition1 = new ArrayList<>();
    	listAllowedStatusTransition1.add("ATTACHED");
    	
    	DocumentTypeConfigurationStatuses status1 = new DocumentTypeConfigurationStatuses();
    	status1.setStorage("PN_TEMPORARY_DOCUMENT");
    	status1.setAllowedStatusTransitions(listAllowedStatusTransition1);
    	
    	List<String> listAllowedStatusTransition2 = new ArrayList<>();
    	
    	DocumentTypeConfigurationStatuses status2 = new DocumentTypeConfigurationStatuses();
    	status2.setStorage("PN_NOTIFIED_DOCUMENTS");
    	status2.setAllowedStatusTransitions(listAllowedStatusTransition2);
    	
    	Map<String, DocumentTypeConfigurationStatuses> map = new HashMap<>();
    	map.put("PRELOADED", status1);
    	map.put("ATTACHED", status2);
    	
    	DocumentTypeConfiguration documentTypeConfiguration = new DocumentTypeConfiguration();
    	documentTypeConfiguration.setName("PN_NOTIFICATION_ATTACHMENTS");
    	documentTypeConfiguration.setStatuses(map);
    	
    	List<DocumentTypeConfiguration> listDocumentTypeConfiguration = new ArrayList<>();
    	listDocumentTypeConfiguration.add(documentTypeConfiguration);
    	
    	StorageConfiguration storageConfiguration = new StorageConfiguration();
    	storageConfiguration.setName("PN_TEMPORARY_DOCUMENT");
    	storageConfiguration.setHotPeriod("1d");
    	storageConfiguration.setRetentionPeriod("1d");
    	
    	List<StorageConfiguration> listStorageConfiguration = new ArrayList<>();
    	listStorageConfiguration.add(storageConfiguration);
    	
    	documentTypesConfigurationsReponse = new DocumentTypesConfigurations();
    	documentTypesConfigurationsReponse.setDocumentsTypes(listDocumentTypeConfiguration);
    	documentTypesConfigurationsReponse.setStorageConfigurations(listStorageConfiguration);
    }



    @Test
    public void readFileFromRepositoryManagerWithIdKo(){
        S3ObjectCreated s3obj = new S3ObjectCreated();
        Oggetto oggetto = new Oggetto();
        oggetto.setKey("111-DDD");
        s3obj.setDetailObject(new Detail());
        s3obj.getDetailObject().setObject(oggetto);


        Mockito.when(documentClientCall.getdocument(Mockito.any())).thenReturn(Mono.error(new DocumentKeyNotPresentException("keyFile")));


        assertThrows( DocumentKeyNotPresentException.class,
                () -> {
                    service.incomingMessageFlow("111-DDD","").block();
                });

    }


    @Test
    public void readFileFromBucketStaging(){
        S3ObjectCreated s3obj = new S3ObjectCreated();
        Oggetto oggetto = new Oggetto();
        oggetto.setKey("111-DDD");
        s3obj.setDetailObject(new Detail());
        s3obj.getDetailObject().setObject(oggetto);

        addFileToBucket("111-DDD");
        ResponseBytes<GetObjectResponse> resp = downloadObjectService.execute("111-DDD","dgs-bing-ss-pnssstagingbucket-28myu2kp62x9");
        assertNotNull(resp);
    }




    @Test
    public void readFileFromBucketNotPresent(){
        S3ObjectCreated s3obj = new S3ObjectCreated();
        Oggetto oggetto = new Oggetto();
        oggetto.setKey("222-DDD");
        s3obj.setDetailObject(new Detail());
        s3obj.getDetailObject().setObject(oggetto);



        DocumentResponse docResp = new DocumentResponse();
        Document doc =new Document();
        doc.setContentType(APPLICATION_PDF);
        docResp.setDocument(doc);
        Mockito.doReturn(Mono.just(docResp)).when(documentClientCall).getdocument(Mockito.any());


        assertThrows( S3BucketException.NoSuchKeyException.class,
                () -> {
                    service.incomingMessageFlow("222-DDD","dgs-bing-ss-pnssstagingbucket-28myu2kp62x9").block();
                });


    }



    @Test
    void readFileFromBucketStagingWriteBuckeHot() {
    	
    	Mockito.doReturn(Mono.just(documentTypesConfigurationsReponse)).when(configurationApiCall).getDocumentsConfigs();
		
		DocumentType documentType = new DocumentType();
		documentType.setTipoDocumento("PN_NOTIFICATION_ATTACHMENTS");
		documentType.setDigitalSignature(true);
		Document doc = new Document();
		doc.setDocumentKey("111-DDD");
		doc.setDocumentType(documentType);
		doc.setDocumentState("PRELOADED");
		doc.setContentType(APPLICATION_PDF);
		DocumentResponse docResp = new DocumentResponse();
		docResp.setDocument(doc);
		
		Mockito.doReturn(Mono.just(docResp)).when(documentClientCall).getdocument(Mockito.any());
		
		addFileToBucket("111-DDD");
		
		Mockito.doReturn(Mono.just(docResp)).when(documentClientCall).patchdocument(Mockito.any(),Mockito.any());
		
		assertNull(service.incomingMessageFlow("111-DDD","dgs-bing-ss-pnssstagingbucket-28myu2kp62x9").block());
    }



    private void addFileToBucket(String fileName) {
        S3ClientBuilder client = S3Client.builder();
        client.endpointOverride(URI.create(testAwsS3Endpoint));
        S3Client s3Client = client.build();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName.ssStageName()).key(fileName).build();

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

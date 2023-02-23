package it.pagopa.pnss.transformation.services;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentResponse;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.exception.ArubaSignExceptionLimitCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.S3BucketException;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.model.Detail;
import it.pagopa.pnss.transformation.model.Oggetto;
import it.pagopa.pnss.transformation.model.S3ObjectCreated;
import it.pagopa.pnss.transformation.service.DownloadObjectService;
import it.pagopa.pnss.transformation.service.OrchestratorSignDocument;
import it.pagopa.pnss.transformation.service.SignServiceSoap;
import it.pagopa.pnss.transformation.sqsread.SQSConsumerService;
import it.pagopa.pnss.transformation.wsdl.TypeOfTransportNotImplemented_Exception;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;

import static it.pagopa.pnss.common.Constant.APPLICATION_PDF;
import static it.pagopa.pnss.common.QueueNameConstant.SIGN_QUEUE_NAME;
import static org.junit.Assert.*;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
public class OrchestratorSignDocumentTest {
    @MockBean
    DocumentClientCall documentClientCall;


    @Autowired
    OrchestratorSignDocument service;
    @Autowired
    BucketName bucketName;

    @Autowired
    DownloadObjectService downloadObjectService;
    @Value("${test.aws.s3.endpoint:#{null}}")
    String testAwsS3Endpoint;



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
        ResponseBytes<GetObjectResponse> resp = downloadObjectService.execute("111-DDD","");
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
                    service.incomingMessageFlow("222-DDD","").block();
                });


    }



    @Test
    public void readFileFromBucketStagingWriteBuckeHot(){
        {


            DocumentResponse docResp = new DocumentResponse();
            Document doc =new Document();
            DocumentType documentType = new DocumentType();
            documentType.setDigitalSignature(true);
            doc.setDocumentType(documentType);
            doc.setContentType(APPLICATION_PDF);
            docResp.setDocument(doc);
            Mockito.doReturn(Mono.just(docResp)).when(documentClientCall).getdocument(Mockito.any());
            addFileToBucket("111-DDD");

            assertNull(service.incomingMessageFlow("111-DDD","").block());
        }
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

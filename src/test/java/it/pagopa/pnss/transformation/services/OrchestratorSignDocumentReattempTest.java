package it.pagopa.pnss.transformation.services;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentResponse;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.exception.ArubaSignExceptionLimitCall;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.model.Oggetto;
import it.pagopa.pnss.transformation.model.S3ObjectCreated;
import it.pagopa.pnss.transformation.service.DownloadObjectService;
import it.pagopa.pnss.transformation.service.OrchestratorSignDocument;
import it.pagopa.pnss.transformation.service.SignServiceSoap;
import it.pagopa.pnss.transformation.wsdl.TypeOfTransportNotImplemented_Exception;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;

import static it.pagopa.pnss.common.Constant.APPLICATION_PDF;
import static org.junit.Assert.*;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
public class OrchestratorSignDocumentReattempTest {
    @MockBean
    DocumentClientCall documentClientCall;

    @MockBean
    SignServiceSoap signServiceSoap;
    @Autowired
    OrchestratorSignDocument service;
    @Autowired
    BucketName bucketName;

    @Autowired
    DownloadObjectService downloadObjectService;
    @Value("${test.aws.s3.endpoint:#{null}}")
    String testAwsS3Endpoint;



    @Test
    public void reattempSignDocumentArubaFault() throws MalformedURLException, TypeOfTransportNotImplemented_Exception, JAXBException {


        DocumentResponse docResp = new DocumentResponse();
        Document doc =new Document();
        doc.setContentType(APPLICATION_PDF);
        docResp.setDocument(doc);
        Mockito.doReturn(Mono.just(docResp)).when(documentClientCall).getdocument(Mockito.any());
        addFileToBucket("666-DDD");

        Mockito.doThrow(new JAXBException("")).when(signServiceSoap).singnPdfDocument(Mockito.any(),Mockito.any());


        assertThrows( ArubaSignExceptionLimitCall.class,
                () -> {
                    service.incomingMessageFlow("666-DDD").block();
                });

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

package it.pagopa.pnss.transformation.services;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentResponse;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.exception.S3BucketException;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.model.Detail;
import it.pagopa.pnss.transformation.model.Oggetto;
import it.pagopa.pnss.transformation.model.S3ObjectCreated;
import it.pagopa.pnss.transformation.service.DownloadObjectService;
import it.pagopa.pnss.transformation.service.OrchestratorSignDocument;
import it.pagopa.pnss.transformation.service.SignServiceSoap;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import static org.junit.Assert.assertThrows;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
public class ReadFromBucketNoPresentTest {
    @MockBean
    DocumentClientCall documentClientCall;

    @MockBean
    SignServiceSoap signServiceSoap;
    @Autowired
    OrchestratorSignDocument service;
    @Autowired
    BucketName bucketName;

    @MockBean
    DownloadObjectService downloadObjectService;
    @Value("${test.aws.s3.endpoint:#{null}}")
    String testAwsS3Endpoint;





    @Test
    public void readFromBucketNotPresent(){
        S3ObjectCreated s3obj = new S3ObjectCreated();
        Oggetto oggetto = new Oggetto();
        oggetto.setKey("111-DDD");
        s3obj.setDetailObject(new Detail());
        s3obj.getDetailObject().setObject(oggetto);

        //Mockito.doReturn(Mono.error(new DocumentkeyNotPresentException("keyFile"))).when(documentClientCall).getdocument(Mockito.any());

        Mockito.doReturn(Mono.just(new DocumentResponse())).when(documentClientCall).getdocument(Mockito.any());
        Mockito.doThrow(NoSuchBucketException.class).when(downloadObjectService).execute(Mockito.any());


        assertThrows( S3BucketException.BucketNotPresentException.class,
                () -> {
                    service.incomingMessageFlow("111-DDD").block();
                });
    }



}

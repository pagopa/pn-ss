package it.pagopa.pnss.transformation.service;

import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
public class ReadFromBucketNoPresentTest {
//    @MockBean
//    DocumentClientCall documentClientCall;
//
//    @MockBean
//    SignServiceSoap signServiceSoap;
//    @Autowired
//    OrchestratorSignDocument service;
//    @Autowired
//    BucketName bucketName;
//
//    @MockBean
//    S3ServiceImpl s3Service;
//    @Value("${test.aws.s3.endpoint:#{null}}")
//    String testAwsS3Endpoint;
//
//
//
//
//
//    @Test
//    public void readFromBucketNotPresent(){
//        S3ObjectDto s3obj = new S3ObjectDto();
//        Oggetto oggetto = new Oggetto();
//        oggetto.setKey("111-DDD");
//        s3obj.setDetailObject(new Detail());
//        s3obj.getDetailObject().setObject(oggetto);
//
//        //Mockito.doReturn(Mono.error(new DocumentkeyNotPresentException("keyFile"))).when(documentClientCall).getdocument(Mockito.any());
//
//        Mockito.doReturn(Mono.just(new DocumentResponse())).when(documentClientCall).getdocument(Mockito.any());
//        Mockito.doThrow(NoSuchBucketException.class).when(s3Service).getObject(Mockito.any(), Mockito.any());
//
//
//        assertThrows( S3BucketException.BucketNotPresentException.class,
//                () -> {
//                    service.incomingMessageFlow("111-DDD","", false).block();
//                });
//    }
//
//
//
}

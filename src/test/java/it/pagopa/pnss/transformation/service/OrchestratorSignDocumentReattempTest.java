package it.pagopa.pnss.transformation.service;

import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
public class OrchestratorSignDocumentReattempTest {
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
//    @Autowired
//    S3ServiceImpl s3Service;
//    @Value("${test.aws.s3.endpoint:#{null}}")
//    String testAwsS3Endpoint;
//
//
//
//    @Test
//    public void reattempSignDocumentArubaFault() throws MalformedURLException, TypeOfTransportNotImplemented_Exception, JAXBException {
//
//
//        DocumentResponse docResp = new DocumentResponse();
//        Document doc =new Document();
//        doc.setContentType(APPLICATION_PDF);
//        DocumentType documentType = new DocumentType();
//        documentType.setTransformations(List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
//        doc.setDocumentType(documentType);
//        docResp.setDocument(doc);
//        Mockito.doReturn(Mono.just(docResp)).when(documentClientCall).getdocument(Mockito.any());
//        addFileToBucket("666-DDD");
//
//        Mockito.doThrow(new JAXBException("")).when(signServiceSoap).signPdfDocument(Mockito.any(), Mockito.any());
//
//
//        assertThrows( ArubaSignExceptionLimitCall.class,
//                () -> {
//                    service.incomingMessageFlow("666-DDD","dgs-bing-ss-pnssstagingbucket-28myu2kp62x9", false).block();
//                });
//
//    }
//
//
//    private void addFileToBucket(String fileName) {
//        S3ClientBuilder client = S3Client.builder();
//        client.endpointOverride(URI.create(testAwsS3Endpoint));
//        S3Client s3Client = client.build();
//        PutObjectRequest request = PutObjectRequest.builder()
//                .bucket(bucketName.ssStageName()).key(fileName).build();
//
//        s3Client.putObject(request, RequestBody.fromBytes(readPdfDocoument()));
//    }
//
//    private byte[] readPdfDocoument() {
//        byte[] byteArray=null;
//        try {
//            InputStream is =  getClass().getResourceAsStream("/PDF_PROVA.pdf");
//            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//
//            int nRead;
//            byte[] data = new byte[16384];
//
//            while ((nRead = is.read(data, 0, data.length)) != -1) {
//                buffer.write(data, 0, nRead);
//            }
//
//            byteArray = buffer.toByteArray();
//
//        } catch (FileNotFoundException e) {
//            System.out.println("File Not found"+e);
//        } catch (IOException e) {
//            System.out.println("IO Ex"+e);
//        }
//        return byteArray;
//
//    }
//
//
}

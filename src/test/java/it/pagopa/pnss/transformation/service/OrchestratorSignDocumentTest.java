package it.pagopa.pnss.transformation.service;

import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Slf4j
public class OrchestratorSignDocumentTest {

//    @Value("${default.internal.x-api-key.value}")
//    private String defaultInteralApiKeyValue;
//
//    @Value("${default.internal.header.x-pagopa-safestorage-cx-id}")
//    private String defaultInternalClientIdValue;
//
//    @MockBean
//    DocumentClientCall documentClientCall;
//    @MockBean
//    ConfigurationApiCall configurationApiCall;
//
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
//    private static final String tipoDocumentoPnNotificationAttachments = "PN_NOTIFICATION_ATTACHMENTS";
//    private static final String statoDocumentoPreloaded = "PRELOADED";
//    private static final String storageClassPnTemporaryDocument = "PN_TEMPORARY_DOCUMENT";
//    private static DocumentTypesConfigurations documentTypesConfigurationsReponse;
//
//    @BeforeEach
//    private void createDocumentsConfigResponse() {
//
//    	List<String> listAllowedStatusTransition1 = new ArrayList<>();
//    	listAllowedStatusTransition1.add("ATTACHED");
//
//    	DocumentTypeConfigurationStatuses status1 = new DocumentTypeConfigurationStatuses();
//    	status1.setStorage(storageClassPnTemporaryDocument);
//    	status1.setAllowedStatusTransitions(listAllowedStatusTransition1);
//
//    	List<String> listAllowedStatusTransition2 = new ArrayList<>();
//
//    	DocumentTypeConfigurationStatuses status2 = new DocumentTypeConfigurationStatuses();
//    	status2.setStorage("PN_NOTIFIED_DOCUMENTS");
//    	status2.setAllowedStatusTransitions(listAllowedStatusTransition2);
//
//    	Map<String, DocumentTypeConfigurationStatuses> map = new HashMap<>();
//    	map.put(statoDocumentoPreloaded, status1);
//    	map.put("ATTACHED", status2);
//
//    	DocumentTypeConfiguration documentTypeConfiguration = new DocumentTypeConfiguration();
//    	documentTypeConfiguration.setName(tipoDocumentoPnNotificationAttachments);
//    	documentTypeConfiguration.setStatuses(map);
//
//    	List<DocumentTypeConfiguration> listDocumentTypeConfiguration = new ArrayList<>();
//    	listDocumentTypeConfiguration.add(documentTypeConfiguration);
//
//    	StorageConfiguration storageConfiguration = new StorageConfiguration();
//    	storageConfiguration.setName(storageClassPnTemporaryDocument);
//    	storageConfiguration.setHotPeriod("3d");
//    	storageConfiguration.setRetentionPeriod("3d");
//
//    	List<StorageConfiguration> listStorageConfiguration = new ArrayList<>();
//    	listStorageConfiguration.add(storageConfiguration);
//
//    	documentTypesConfigurationsReponse = new DocumentTypesConfigurations();
//    	documentTypesConfigurationsReponse.setDocumentsTypes(listDocumentTypeConfiguration);
//    	documentTypesConfigurationsReponse.setStorageConfigurations(listStorageConfiguration);
//    }
//
//
//
//    @Test
//    public void readFileFromRepositoryManagerWithIdKo(){
//        S3ObjectDto s3obj = new S3ObjectDto();
//        Oggetto oggetto = new Oggetto();
//        oggetto.setKey("111-DDD");
//        s3obj.setDetailObject(new Detail());
//        s3obj.getDetailObject().setObject(oggetto);
//
//
//        Mockito.when(documentClientCall.getdocument(Mockito.any())).thenReturn(Mono.error(new DocumentKeyNotPresentException("keyFile")));
//
//
//        assertThrows( DocumentKeyNotPresentException.class,
//                () -> {
//                    service.incomingMessageFlow("111-DDD","", false).block();
//                });
//
//    }
//
//
//    @Test
//    public void readFileFromBucketStaging(){
//        S3ObjectDto s3obj = new S3ObjectDto();
//        Oggetto oggetto = new Oggetto();
//        oggetto.setKey("111-DDD");
//        s3obj.setDetailObject(new Detail());
//        s3obj.getDetailObject().setObject(oggetto);
//
//        addFileToBucket("111-DDD");
//        ResponseBytes<GetObjectResponse> resp = s3Service.getObject("111-DDD", "dgs-bing-ss-pnssstagingbucket-28myu2kp62x9");
//        assertNotNull(resp);
//    }
//
//
//
//
//    @Test
//    public void readFileFromBucketNotPresent(){
//        S3ObjectDto s3obj = new S3ObjectDto();
//        Oggetto oggetto = new Oggetto();
//        oggetto.setKey("222-DDD");
//        s3obj.setDetailObject(new Detail());
//        s3obj.getDetailObject().setObject(oggetto);
//
//
//
//        DocumentResponse docResp = new DocumentResponse();
//        Document doc =new Document();
//        doc.setContentType(APPLICATION_PDF);
//        docResp.setDocument(doc);
//        Mockito.doReturn(Mono.just(docResp)).when(documentClientCall).getdocument(Mockito.any());
//
//
//        assertThrows( S3BucketException.NoSuchKeyException.class,
//                () -> {
//                    service.incomingMessageFlow("222-DDD","dgs-bing-ss-pnssstagingbucket-28myu2kp62x9", false).block();
//                });
//
//
//    }
//
//
//
//    @Test
//    void readFileFromBucketStagingWriteBuckeHot() {
//
//    	log.debug("readFileFromBucketStagingWriteBuckeHot() : START : "
//    			+ "security configuration : defaultInternalClientIdValue {} : defaultInteralApiKeyValue {}",
//    			defaultInternalClientIdValue, defaultInteralApiKeyValue);
//
//    	Mockito.doReturn(Mono.just(documentTypesConfigurationsReponse)).when(configurationApiCall).getDocumentsConfigs
//    	(defaultInternalClientIdValue, defaultInteralApiKeyValue);
//
//		DocumentType documentType = new DocumentType();
//		documentType.setTipoDocumento(tipoDocumentoPnNotificationAttachments);
//		documentType.setTransformations(List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
//		Document doc = new Document();
//		doc.setDocumentKey("111-DDD");
//		doc.setDocumentType(documentType);
//		doc.setDocumentState(statoDocumentoPreloaded);
//		doc.setContentType(APPLICATION_PDF);
//		DocumentResponse docResp = new DocumentResponse();
//		docResp.setDocument(doc);
//
//		Mockito.doReturn(Mono.just(docResp)).when(documentClientCall).getdocument(Mockito.any());
//
//		addFileToBucket("111-DDD");
//
//		Mockito.doReturn(Mono.just(docResp)).when(documentClientCall).patchdocument(Mockito.any(), Mockito.any(), Mockito.any(),Mockito
//		.any());
//
//		assertNull(service.incomingMessageFlow("111-DDD","dgs-bing-ss-pnssstagingbucket-28myu2kp62x9", false).block());
//    }
//
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

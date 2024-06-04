package it.pagopa.pnss.repositorymanager.rest.internal;

import static it.pagopa.pnss.common.constant.Constant.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.model.dto.MacchinaStatiValidateStatoResponseDto;
import it.pagopa.pnss.common.model.pojo.DocumentStatusChange;
import it.pagopa.pnss.common.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pnss.common.retention.RetentionService;
import it.pagopa.pnss.utils.IgnoredUpdateMetadataConfigTestSetup;
import it.pagopa.pnss.repositorymanager.entity.CurrentStatusEntity;
import it.pagopa.pnss.repositorymanager.service.DocumentService;
import it.pagopa.pnss.transformation.service.S3Service;
import lombok.CustomLog;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.DocTypeEntity;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient(timeout = "36000")
@CustomLog
public class DocumentInternalApiControllerTest extends IgnoredUpdateMetadataConfigTestSetup {

    @Value("${test.aws.s3.endpoint:#{null}}")
    String testAwsS3Endpoint;

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private BucketName bucketName;
	@Autowired
	private S3Client s3TestClient;
	@SpyBean
	private S3Service s3Service;
	@SpyBean
	private DocumentService documentService;

    private static final String BASE_PATH = "/safestorage/internal/v1/documents";
    private static final String BASE_PATH_WITH_PARAM = String.format("%s/{documentKey}", BASE_PATH);
	private static final String DOCTYPE_ID_LEGAL_FACTS = "PN_NOTIFICATION_ATTACHMENTS";
	private static final String PARTITION_ID_ENTITY = "documentKeyEnt";
	private static final String PARTITION_ID_DEFAULT = PARTITION_ID_ENTITY;
	private static final String PARTITION_ID_NO_EXISTENT = "documentKey_bad";
	private static final String CHECKSUM = "91375e9e5a9510087606894437a6a382fa5bc74950f932e2b85a788303cf5ba0";

	private static DocumentInput documentInput;
	private static DocumentChanges documentChanges;
	private static DynamoDbTable<DocumentEntity> dynamoDbTable;

	@MockBean
	private CallMacchinaStati callMacchinaStati;
	@MockBean
	private RetentionService retentionService;
	@MockBean
	private UserConfigurationClientCall userConfigurationClientCall;

	private static void insertDocumentEntity(String documentKey) {
		log.info("execute insertDocumentEntity()");

		List<String> allowedStatusTransitions1 = new ArrayList<>();
		allowedStatusTransitions1.add("AVAILABLE");

		CurrentStatusEntity currentStatus1 = new CurrentStatusEntity();
		currentStatus1.setStorage(DOCTYPE_ID_LEGAL_FACTS);
		currentStatus1.setAllowedStatusTransitions(allowedStatusTransitions1);
		currentStatus1.setTechnicalState("SAVED");

		Map<String, CurrentStatusEntity> statuses1 = new HashMap<>();
		statuses1.put("SAVED", currentStatus1);

		DocTypeEntity docTypeEntity = new DocTypeEntity();
		docTypeEntity.setTipoDocumento(DOCTYPE_ID_LEGAL_FACTS);
		docTypeEntity.setStatuses(statuses1);
		log.info("execute insertDocumentEntity() : docTypeEntity : {}", docTypeEntity);

		var documentEntity = new DocumentEntity();
		documentEntity.setDocumentKey(documentKey);
		documentEntity.setDocumentType(docTypeEntity);
		documentEntity.setContentLenght(new BigDecimal(50));
		documentEntity.setLastStatusChangeTimestamp(OffsetDateTime.now());
		documentEntity.setDocumentState(SAVED);
		documentEntity.setDocumentLogicalState(AVAILABLE);
		dynamoDbTable.putItem(builder -> builder.item(documentEntity));
	}

	private static void insertDocumentEntity(DocumentEntity documentEntity) {
		dynamoDbTable.putItem(builder -> builder.item(documentEntity));
	}

	@BeforeAll
	public static void insertDefaultDocument(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient,
			@Autowired RepositoryManagerDynamoTableName gestoreRepositoryDynamoDbTableName) {
		log.info("execute insertDefaultDocument()");
		dynamoDbTable = dynamoDbEnhancedClient.table(
//    			DynamoTableNameConstant.DOCUMENT_TABLE_NAME, 
				gestoreRepositoryDynamoDbTableName.documentiName(), TableSchema.fromBean(DocumentEntity.class));
		insertDocumentEntity(PARTITION_ID_ENTITY);
	}

	@BeforeEach
	public void createDocument() {
		log.info("execute createDocument()");

		List<String> allowedStatusTransitions1 = new ArrayList<>();
		allowedStatusTransitions1.add("AVAILABLE");

		CurrentStatus currentStatus1 = new CurrentStatus();
		currentStatus1.setStorage(DOCTYPE_ID_LEGAL_FACTS);
		currentStatus1.setAllowedStatusTransitions(allowedStatusTransitions1);

		Map<String, CurrentStatus> statuses1 = new HashMap<>();
		statuses1.put("PRELOADED", currentStatus1);

		DocumentType docTypes = new DocumentType();
		docTypes.setTipoDocumento(DOCTYPE_ID_LEGAL_FACTS);
		docTypes.setChecksum(DocumentType.ChecksumEnum.SHA256);
		docTypes.setInitialStatus("SAVED");
		docTypes.setStatuses(statuses1);
		docTypes.setInformationClassification(DocumentType.InformationClassificationEnum.HC);
		docTypes.setTransformations(List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
		docTypes.setTimeStamped(DocumentType.TimeStampedEnum.STANDARD);
		log.info("execute createDocument() : docType : {}", docTypes);

		documentInput = new DocumentInput();
		documentInput.setDocumentKey(PARTITION_ID_DEFAULT);
		documentInput.setDocumentState(FREEZED);
		documentInput.setRetentionUntil("2032-04-12T12:32:04.000Z");
		documentInput.setCheckSum(CHECKSUM);
		documentInput.setContentType("xxxxx");
		documentInput.setDocumentType(DOCTYPE_ID_LEGAL_FACTS);
		documentInput.setContentLenght(new BigDecimal(100));
		log.info("execute createDocument() : documentInput : {}", documentInput);

		documentChanges = new DocumentChanges();
		documentChanges.setDocumentState(SAVED);
		documentChanges.setContentLenght(new BigDecimal(50));
	}

	@Test
	// codice test: DCSS.101.1
	void postItem() {

		EntityExchangeResult<DocumentResponse> resultPreInsert = webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(documentInput.getDocumentKey()))
				.accept(APPLICATION_JSON).exchange().expectStatus().isOk().expectBody(DocumentResponse.class)
				.returnResult();

		log.info("\n Test 1 (postItem) resultPreInsert {} \n", resultPreInsert);

		if (resultPreInsert == null || resultPreInsert.getResponseBody() == null
				|| resultPreInsert.getResponseBody().getDocument() == null) {
			webTestClient.post().uri(BASE_PATH).accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
					.body(BodyInserters.fromValue(documentInput)).exchange().expectStatus().isOk();
		}

		log.info("\n Test 1 (postItem) passed \n");
	}

	@Test
	// codice test: DCSS.101.2
	void postItemPartitionKeyDuplicated() {

		EntityExchangeResult<DocumentResponse> resultPreInsert = webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(documentInput.getDocumentKey()))
				.accept(APPLICATION_JSON).exchange().expectStatus().isOk().expectBody(DocumentResponse.class)
				.returnResult();

		log.info("\n Test 2 (postItemPartitionKeyDuplicated) resultPreInsert {} \n", resultPreInsert);

		if (resultPreInsert != null && resultPreInsert.getResponseBody() != null
				&& resultPreInsert.getResponseBody().getDocument() != null) {

			webTestClient.post().uri(BASE_PATH).accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
					.body(BodyInserters.fromValue(documentInput)).exchange().expectStatus()
					.isEqualTo(HttpStatus.CONFLICT);
		}

		log.info("\n Test 2 (postItemPartitionKeyDuplicated) passed \n");

	}

	@Test
	// codice test: DCSS.101.2
	void postItemIncorrectParameter() {

		documentInput.setDocumentKey(null);

		webTestClient.post().uri(BASE_PATH).accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
				.body(BodyInserters.fromValue(documentInput)).exchange().expectStatus()
				.isEqualTo(HttpStatus.BAD_REQUEST);

		log.info("\n Test 2 (postItemIncorrectParameter) passed \n");

	}

	@Test
	// codice test: DCSS.100.1
	void getItem() {

		webTestClient.get().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT))
				.accept(APPLICATION_JSON).exchange().expectStatus().isOk().expectBody(DocumentResponse.class);

		log.info("\n Test 3 (getItem) passed \n");

	}

	@Test
	// codice test: DCSS.100.2
	void getItemNoExistentPartitionKey() {

		webTestClient.get().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_NO_EXISTENT))
				.accept(APPLICATION_JSON).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);

		log.info("\n Test 4 (getItemNoExistentPartitionKey) passed \n");

	}

	@Test
	// codice test: DCSS.100.3
	void getItemIncorrectParameters() {

		webTestClient.get().uri(BASE_PATH).accept(APPLICATION_JSON).exchange().expectStatus()
				.isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);

		log.info("\n Test 5 (getItemIncorrectParameters) passed \n");

	}

	@Test
	// codice test: DCSS.102.1
	void patchItem() {

		log.warn("DocumentInternalApiControllerTest.patchItem() : START");

		String documentKey = "docKeyExecutePatch";

		List<String> allowedStatusTransitions = new ArrayList<>();
		allowedStatusTransitions.add("ATTACHED");

		CurrentStatusEntity currentStatusAttached = new CurrentStatusEntity();
		currentStatusAttached.setStorage("PN_NOTIFIED_DOCUMENTS");
		currentStatusAttached.setTechnicalState(ATTACHED);

		CurrentStatusEntity currentStatusPreloaded = new CurrentStatusEntity();
		currentStatusPreloaded.setStorage("PN_TEMPORARY_DOCUMENT");
		currentStatusPreloaded.setAllowedStatusTransitions(allowedStatusTransitions);
		currentStatusPreloaded.setTechnicalState(AVAILABLE);

		Map<String, CurrentStatusEntity> statuses1 = new HashMap<>();
		statuses1.put("ATTACHED", currentStatusAttached);
		statuses1.put("PRELOADED", currentStatusPreloaded);

		DocTypeEntity docTypeEntity = new DocTypeEntity();
		docTypeEntity.setTipoDocumento(DOCTYPE_ID_LEGAL_FACTS);
		docTypeEntity.setStatuses(statuses1);

		var documentEntity = new DocumentEntity();
		documentEntity.setDocumentKey(documentKey);
		documentEntity.setDocumentType(docTypeEntity);
		documentEntity.setContentLenght(new BigDecimal(50));
		documentEntity.setDocumentState(PRELOADED);
		documentEntity.setDocumentLogicalState(AVAILABLE);

		insertDocumentEntity(documentEntity);
		addFileToBucket(documentKey, bucketName.ssHotName());

		DocumentChanges docChanges = new DocumentChanges();
		docChanges.setDocumentState(ATTACHED);
		docChanges.setContentLenght(new BigDecimal(60));
		docChanges.setLastStatusChangeTimestamp(OffsetDateTime.now().minus(10, ChronoUnit.MINUTES));

		when(userConfigurationClientCall.getUser("pn-test")).thenReturn(Mono.just(new UserConfigurationResponse().userConfiguration(new UserConfiguration().name("pn-test").apiKey("pn-test_api_key"))));
		when(callMacchinaStati.statusValidation(any(DocumentStatusChange.class))).thenReturn(Mono.just(new MacchinaStatiValidateStatoResponseDto()));
		documentEntity.setVersion(1L);
		when(retentionService.setRetentionPeriodInBucketObjectMetadata(anyString(), anyString(), any(), any(), anyString())).thenReturn(Mono.just(documentEntity));

		webTestClient.patch()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(documentKey))
				.header("x-pagopa-safestorage-cx-id", "pn-test")
				.header("x-api-key", "pn-test_api_key")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.body(BodyInserters.fromValue(docChanges))
				.exchange().expectStatus().isOk();

		log.info("\n Test 6 (patchItem) passed \n");
	}

	@Test
	void patchItemIgnoreS3UpdateMetadataOk() {

		log.warn("DocumentInternalApiControllerTest.patchItem() : START");

		String documentKey = "fileKeyToIgnoreUpdateMetadata";

		List<String> allowedStatusTransitions = new ArrayList<>();
		allowedStatusTransitions.add("ATTACHED");

		CurrentStatusEntity currentStatusAttached = new CurrentStatusEntity();
		currentStatusAttached.setStorage("PN_NOTIFIED_DOCUMENTS");
		currentStatusAttached.setTechnicalState(ATTACHED);

		CurrentStatusEntity currentStatusPreloaded = new CurrentStatusEntity();
		currentStatusPreloaded.setStorage("PN_TEMPORARY_DOCUMENT");
		currentStatusPreloaded.setAllowedStatusTransitions(allowedStatusTransitions);
		currentStatusPreloaded.setTechnicalState(AVAILABLE);

		Map<String, CurrentStatusEntity> statuses1 = new HashMap<>();
		statuses1.put("ATTACHED", currentStatusAttached);
		statuses1.put("PRELOADED", currentStatusPreloaded);

		DocTypeEntity docTypeEntity = new DocTypeEntity();
		docTypeEntity.setTipoDocumento(DOCTYPE_ID_LEGAL_FACTS);
		docTypeEntity.setStatuses(statuses1);

		var documentEntity = new DocumentEntity();
		documentEntity.setDocumentKey(documentKey);
		documentEntity.setDocumentType(docTypeEntity);
		documentEntity.setContentLenght(new BigDecimal(50));
		documentEntity.setDocumentState(PRELOADED);
		documentEntity.setDocumentLogicalState(AVAILABLE);

		insertDocumentEntity(documentEntity);
		addFileToBucket(documentKey, bucketName.ssHotName());

		DocumentChanges docChanges = new DocumentChanges();
		docChanges.setDocumentState(ATTACHED);
		docChanges.setContentLenght(new BigDecimal(60));
		docChanges.setLastStatusChangeTimestamp(OffsetDateTime.now().minus(10, ChronoUnit.MINUTES));

		when(userConfigurationClientCall.getUser("pn-test")).thenReturn(Mono.just(new UserConfigurationResponse().userConfiguration(new UserConfiguration().name("pn-test").apiKey("pn-test_api_key"))));
		when(callMacchinaStati.statusValidation(any(DocumentStatusChange.class))).thenReturn(Mono.just(new MacchinaStatiValidateStatoResponseDto()));
		documentEntity.setVersion(1L);
		when(retentionService.setRetentionPeriodInBucketObjectMetadata(anyString(), anyString(), any(), any(), anyString())).thenReturn(Mono.just(documentEntity));

		webTestClient.patch()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(documentKey))
				.header("x-pagopa-safestorage-cx-id", "pn-test")
				.header("x-api-key", "pn-test_api_key")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.body(BodyInserters.fromValue(docChanges))
				.exchange().expectStatus().isOk();

		verify(s3Service, never()).putObjectTagging(anyString(), anyString(), any());
		verify(retentionService, never()).setRetentionPeriodInBucketObjectMetadata(anyString(), anyString(), any(), any(), anyString());

		//Clean-up
		s3TestClient.deleteObject(builder -> builder.bucket(bucketName.ssHotName()).key(documentKey));
		log.info("\n Test 6 (patchItem) passed \n");
	}

	@Test
	void patchItemIdempotenceOk() {

		DocumentChanges docChanges = new DocumentChanges();
		docChanges.setDocumentState(SAVED);
		docChanges.setContentLenght(new BigDecimal(50));

		webTestClient.patch().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_ENTITY))
				.accept(APPLICATION_JSON).contentType(APPLICATION_JSON).body(BodyInserters.fromValue(docChanges))
				.exchange().expectStatus().isEqualTo(HttpStatus.OK);
		log.info("\n Test 6 (patchItem) passed \n");
	}

	@Test
	void patchItemLastStatusChangeOk() {

		DocumentChanges docChanges = new DocumentChanges();
		docChanges.setDocumentState(AVAILABLE);
		docChanges.setContentLenght(new BigDecimal(60));
		docChanges.setLastStatusChangeTimestamp(OffsetDateTime.now().minus(10, ChronoUnit.MINUTES));

		webTestClient.patch().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_ENTITY))
				.accept(APPLICATION_JSON).contentType(APPLICATION_JSON).body(BodyInserters.fromValue(docChanges))
				.exchange().expectStatus().isEqualTo(HttpStatus.OK);
		log.info("\n Test 6 (patchItem) passed \n");
	}

	@Test
	// codice test: DCSS.102.2
	void patchItemNoExistentKey() {

		webTestClient.patch().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_NO_EXISTENT))
				.accept(APPLICATION_JSON).contentType(APPLICATION_JSON).body(BodyInserters.fromValue(documentInput))
				.exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);

		log.info("\n Test 7 (patchItemNoExistentKey) passed \n");

	}

	@Test
	// codice test: DCSS.102.3
	void patchItemIncorretcParameters() {

		webTestClient.patch().uri(BASE_PATH).accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
				.body(BodyInserters.fromValue(documentInput)).exchange().expectStatus()
				.isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);

		log.info("\n Test 8 (patchItemIncorretcParameters) passed \n");

	}

	@Test
	// codice test: DCSS.103.1
	void deleteItem() {

		webTestClient.delete().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT))
				.accept(APPLICATION_JSON).exchange().expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

		log.info("\n Test 9 (deleteItem) passed \n");

	}

	@Test
	// codice test: DCSS.103.2
	void deleteItemNoExistentKey() {

		webTestClient.delete().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_NO_EXISTENT))
				.accept(APPLICATION_JSON).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);

		log.info("\n Test 10 (deleteItemNoExistentKey) passed \n");

	}

	@Test
	// codice test: DCSS.103.3
	void deleteItemIncorrectParameter() {

		webTestClient.delete().uri(BASE_PATH).accept(APPLICATION_JSON).exchange().expectStatus()
				.isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);

		log.info("\n Test 10 (deleteItemIncorrectParameter) passed \n");

    }

	private void addFileToBucket(String fileName, String bucketName) {
		byte[] fileBytes = readPdfDocoument();
		PutObjectRequest request = PutObjectRequest.builder()
				.bucket(bucketName)
				.key(fileName)
				.contentMD5(new String(Base64.encodeBase64(DigestUtils.md5(fileBytes)))).build();
		s3TestClient.putObject(request, RequestBody.fromBytes(fileBytes));
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
        	log.debug("File Not found",e);
        } catch (IOException e) {
            log.debug("IO Ex", e);
        }
        return byteArray;

    }

}
package it.pagopa.pnss.repositorymanager.rest.internal;

import static it.pagopa.pnss.common.constant.Constant.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.common.client.exception.RetentionException;
import it.pagopa.pnss.common.exception.InvalidNextStatusException;
import it.pagopa.pnss.common.model.dto.MacchinaStatiValidateStatoResponseDto;
import it.pagopa.pnss.common.model.pojo.DocumentStatusChange;
import it.pagopa.pnss.common.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pnss.common.retention.RetentionService;
import it.pagopa.pnss.repositorymanager.exception.IllegalDocumentStateException;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.utils.IgnoredUpdateMetadataConfigTestSetup;
import it.pagopa.pnss.repositorymanager.entity.CurrentStatusEntity;
import it.pagopa.pnss.transformation.service.S3Service;
import lombok.CustomLog;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
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
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
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

    private static final String BASE_PATH = "/safestorage/internal/v1/documents";
    private static final String BASE_PATH_WITH_PARAM = String.format("%s/{documentKey}", BASE_PATH);
	private static final String DOCTYPE_ID_LEGAL_FACTS = "PN_NOTIFICATION_ATTACHMENTS";
	private static final String PARTITION_ID_ENTITY = "documentKeyEnt";
	private static final String PARTITION_ID_ENTITY_TAGS = "documentKeyEntTags";
	private static final String PARTITION_ID_ENTITY_TAGS_DELETE = "documentKeyEntTagsDelete";
	private static final String PARTITION_ID_DEFAULT = PARTITION_ID_ENTITY;
	private static final String PARTITION_ID_DEFAULT_TAGS = PARTITION_ID_ENTITY_TAGS;
	private static final String PARTITION_ID_DEFAULT_TAGS_DELETE = PARTITION_ID_ENTITY_TAGS_DELETE;
	private static final String PARTITION_ID_NO_EXISTENT = "documentKey_bad";
	private static final String CHECKSUM = "91375e9e5a9510087606894437a6a382fa5bc74950f932e2b85a788303cf5ba0";

	private static DocumentInput documentInput;
	private static DocumentInput documentInputTags;
	private static DocumentChanges documentChanges;
	private static DynamoDbTable<DocumentEntity> dynamoDbTable;

	@MockBean
	private CallMacchinaStati callMacchinaStati;
	@MockBean
	private RetentionService retentionService;
	@MockBean
	private UserConfigurationClientCall userConfigurationClientCall;
	@Autowired
	private DocumentInternalApiController documentInternalApiController;


	private static Map<String, List<String>> createTagsList(){
		Map<String, List<String>> tags = new HashMap<>();

		List<String> valuesForKey1 = new ArrayList<>();
		valuesForKey1.add("value_1");
		valuesForKey1.add("value_7");
		tags.put("key_1", valuesForKey1);

		List<String> valuesForKey2 = new ArrayList<>();
		valuesForKey2.add("value_2");
		tags.put("key_2", valuesForKey2);

		List<String> valuesForKey3 = new ArrayList<>();
		valuesForKey3.add("value_3");
		valuesForKey3.add("value_9");
		valuesForKey3.add("value_8");
		tags.put("key_3", valuesForKey3);

		List<String> valuesForKey4 = new ArrayList<>();
		valuesForKey4.add("value_4");
		tags.put("key_4", valuesForKey4);
		return tags;
	}

	private static void insertDocumentEntity() {
		log.info("execute insertDocumentEntity()");

		DocTypeEntity docTypeEntity = getDocTypeEntity();
		log.info("execute insertDocumentEntity() : docTypeEntity : {}", docTypeEntity);

		var documentEntity = new DocumentEntity();
		documentEntity.setDocumentKey(DocumentInternalApiControllerTest.PARTITION_ID_ENTITY);
		documentEntity.setDocumentType(docTypeEntity);
		documentEntity.setContentLenght(new BigDecimal(50));
		documentEntity.setLastStatusChangeTimestamp(OffsetDateTime.now());
		documentEntity.setDocumentState(SAVED);
		documentEntity.setDocumentLogicalState(AVAILABLE);
		dynamoDbTable.putItem(builder -> builder.item(documentEntity));
	}

	private static DocTypeEntity getDocTypeEntity() {
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
		return docTypeEntity;
	}

	private static void insertDocumentEntityWithTags(String documentKey) {
		log.info("execute insertDocumentEntity()");

		DocTypeEntity docTypeEntity = getDocTypeEntity();
		log.info("execute insertDocumentEntityTags() : docTypeEntity : {}", docTypeEntity);

		var documentEntity = new DocumentEntity();
		documentEntity.setDocumentKey(documentKey);
		documentEntity.setDocumentType(docTypeEntity);
		documentEntity.setContentLenght(new BigDecimal(50));
		documentEntity.setLastStatusChangeTimestamp(OffsetDateTime.now());
		documentEntity.setDocumentState(SAVED);
		documentEntity.setDocumentLogicalState(AVAILABLE);
		documentEntity.setTags(createTagsList());
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
		insertDocumentEntity();
		insertDocumentEntityWithTags(PARTITION_ID_ENTITY_TAGS);
		insertDocumentEntityWithTags(PARTITION_ID_DEFAULT_TAGS_DELETE);
	}

	@BeforeEach
	public void setUp() {

		documentInputTags = createDocumentWithTags();
		documentInput = createDocument();

	}

	public DocumentInput createDocument() {
		log.info("execute createDocument()");

		DocumentType docTypes = getDocumentType();
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

		return documentInput;
	}

	private static DocumentType getDocumentType() {
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
		docTypes.setTransformations(List.of("SIGN_AND_TIMEMARK"));
		docTypes.setTimeStamped(DocumentType.TimeStampedEnum.STANDARD);
		return docTypes;
	}

	public DocumentInput createDocumentWithTags() {
		log.info("execute createDocument()");

		DocumentType docTypes = getDocumentType();
		log.info("execute createDocument() : docType : {}", docTypes);

		documentInputTags = new DocumentInput();
		documentInputTags.setDocumentKey(PARTITION_ID_DEFAULT_TAGS);
		documentInputTags.setDocumentState(FREEZED);
		documentInputTags.setRetentionUntil("2032-04-12T12:32:04.000Z");
		documentInputTags.setCheckSum(CHECKSUM);
		documentInputTags.setContentType("xxxxx");
		documentInputTags.setDocumentType(DOCTYPE_ID_LEGAL_FACTS);
		documentInputTags.setContentLenght(new BigDecimal(100));
		documentInputTags.setTags(createTagsList());

		log.info("execute createDocument() : documentInputTags : {}", documentInputTags);

		documentChanges = new DocumentChanges();
		documentChanges.setDocumentState(SAVED);
		documentChanges.setContentLenght(new BigDecimal(50));
		return documentInputTags;
	}

	@Test
	// codice test: DCSS.101.1
	void postItem() {

		EntityExchangeResult<DocumentResponse> resultPreInsert = webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(documentInput.getDocumentKey()))
				.accept(APPLICATION_JSON).exchange().expectStatus().isOk().expectBody(DocumentResponse.class)
				.returnResult();

		log.info("\n Test 1 (postItem) resultPreInsert {} \n", resultPreInsert);

		if (resultPreInsert.getResponseBody() == null || resultPreInsert.getResponseBody().getDocument() == null) {
			webTestClient.post().uri(BASE_PATH).accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
					.body(BodyInserters.fromValue(documentInput)).exchange().expectStatus().isOk();
		}

		log.info("\n Test 1 (postItem) passed \n");
	}
	@Test
		// codice test: DCSS.101.1
	void postItemWithTags() {
log.info("documentInputTags {}", documentInputTags);
		EntityExchangeResult<DocumentResponse> resultPreInsert = webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(documentInputTags.getDocumentKey()))
				.accept(APPLICATION_JSON).exchange().expectStatus().isOk().expectBody(DocumentResponse.class)
				.returnResult();

		log.info("\n Test 1 (postItem) resultPreInsert {} \n", resultPreInsert);

		if (resultPreInsert.getResponseBody() == null || resultPreInsert.getResponseBody().getDocument() == null) {
			webTestClient.post().uri(BASE_PATH).accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
					.body(BodyInserters.fromValue(documentInputTags)).exchange().expectStatus().isOk();
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

		if (resultPreInsert.getResponseBody() != null && resultPreInsert.getResponseBody().getDocument() != null) {

			webTestClient.post().uri(BASE_PATH).accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
					.body(BodyInserters.fromValue(documentInput)).exchange().expectStatus()
					.isEqualTo(HttpStatus.CONFLICT);
		}

		log.info("\n Test 2 (postItemPartitionKeyDuplicated) passed \n");

	}

	@Test
		// codice test: DCSS.101.2
	void postItemTagsPartitionKeyDuplicated() {

		EntityExchangeResult<DocumentResponse> resultPreInsert = webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(documentInputTags.getDocumentKey()))
				.accept(APPLICATION_JSON).exchange().expectStatus().isOk().expectBody(DocumentResponse.class)
				.returnResult();

		log.info("\n Test 2 (postItemPartitionKeyDuplicated) resultPreInsert {} \n", resultPreInsert);

		if (resultPreInsert.getResponseBody() != null && resultPreInsert.getResponseBody().getDocument() != null) {

			webTestClient.post().uri(BASE_PATH).accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
					.body(BodyInserters.fromValue(documentInputTags)).exchange().expectStatus()
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
		// codice test: DCSS.100.1
	void getItemWithTags() {

		webTestClient.get().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT_TAGS))
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

		DocTypeEntity docTypeEntity = getTypeEntity();

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
		docChanges.setLastStatusChangeTimestamp(OffsetDateTime.now().minusMinutes(10));

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
				.exchange().expectStatus().isOk()
				.expectBody(DocumentResponse.class);

		log.info("\n Test 6 (patchItem) passed \n");
	}

	private static DocTypeEntity getTypeEntity() {
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
		return docTypeEntity;
	}

	@Test
	void patchItemWithTags() {

		log.warn("DocumentInternalApiControllerTest.patchItemWithTags() : START");

		String documentKey = "docKeyExecutePatchTags";

		DocTypeEntity docTypeEntity = getTypeEntity();

		var documentEntity = new DocumentEntity();
		documentEntity.setDocumentKey(documentKey);
		documentEntity.setDocumentType(docTypeEntity);
		documentEntity.setContentLenght(new BigDecimal(50));
		documentEntity.setDocumentState(PRELOADED);
		documentEntity.setDocumentLogicalState(AVAILABLE);
		documentEntity.setTags(createTagsList());


		insertDocumentEntity(documentEntity);
		addFileToBucket(documentKey, bucketName.ssHotName());

		DocumentChanges docChanges = new DocumentChanges();
		docChanges.setDocumentState(ATTACHED);
		docChanges.setContentLenght(new BigDecimal(60));
		docChanges.setLastStatusChangeTimestamp(OffsetDateTime.now().minusMinutes(10));

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

		//The fileKey is in ignored-update-metadata.csv file
		String documentKey = "fileKeyToIgnoreUpdateMetadata";

		DocTypeEntity docTypeEntity = getTypeEntity();

		var documentEntity = new DocumentEntity();
		documentEntity.setDocumentKey(documentKey);
		documentEntity.setDocumentType(docTypeEntity);
		documentEntity.setContentLenght(new BigDecimal(50));
		documentEntity.setDocumentState(PRELOADED);
		documentEntity.setDocumentLogicalState(AVAILABLE);

		insertDocumentEntity(documentEntity);

		DocumentChanges docChanges = new DocumentChanges();
		docChanges.setDocumentState(ATTACHED);
		docChanges.setContentLenght(new BigDecimal(60));
		docChanges.setLastStatusChangeTimestamp(OffsetDateTime.now().minusMinutes(10));

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

		verify(s3Service, times(1)).headObject(anyString(), anyString());
		verify(s3Service, never()).putObjectTagging(anyString(), anyString(), any());
		verify(retentionService, never()).setRetentionPeriodInBucketObjectMetadata(anyString(), anyString(), any(), any(), anyString());

		log.info("\n Test 6 (patchItem) passed \n");
	}

	@Test
	void patchItemWithTagsIgnoreS3UpdateMetadataOk() {

		log.warn("DocumentInternalApiControllerTest.patchItemWithTagsIgnoreS3UpdateMetadataOk() : START");

		//The fileKey is in ignored-update-metadata.csv file
		String documentKey = "fileKeyToIgnoreUpdateMetadata3";

		DocTypeEntity docTypeEntity = getTypeEntity();

		var documentEntity = new DocumentEntity();
		documentEntity.setDocumentKey(documentKey);
		documentEntity.setDocumentType(docTypeEntity);
		documentEntity.setContentLenght(new BigDecimal(50));
		documentEntity.setDocumentState(PRELOADED);
		documentEntity.setDocumentLogicalState(AVAILABLE);
		documentEntity.setTags(createTagsList());


		insertDocumentEntity(documentEntity);

		DocumentChanges docChanges = new DocumentChanges();
		docChanges.setDocumentState(ATTACHED);
		docChanges.setContentLenght(new BigDecimal(60));
		docChanges.setLastStatusChangeTimestamp(OffsetDateTime.now().minusMinutes(10));

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

		verify(s3Service, times(1)).headObject(anyString(), anyString());
		verify(s3Service, never()).putObjectTagging(anyString(), anyString(), any());
		verify(retentionService, never()).setRetentionPeriodInBucketObjectMetadata(anyString(), anyString(), any(), any(), anyString());

		log.info("\n Test 6 (patchItem) passed \n");
	}

	@Test
	void patchItemIgnoreS3UpdateMetadata_FilePresentInS3() {

		log.warn("DocumentInternalApiControllerTest.patchItem() : START");

		//The fileKey is in ignored-update-metadata.csv file
		String documentKey = "fileKeyToIgnoreUpdateMetadata2";

		DocTypeEntity docTypeEntity = getTypeEntity();

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
		docChanges.setLastStatusChangeTimestamp(OffsetDateTime.now().minusMinutes(10));

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

		verify(s3Service, times(1)).putObjectTagging(anyString(), anyString(), any());
		verify(retentionService, times(1)).setRetentionPeriodInBucketObjectMetadata(anyString(), anyString(), any(), any(), anyString());

		//Clean-up
		s3TestClient.deleteObject(builder -> builder.bucket(bucketName.ssHotName()).key(documentKey));
		log.info("\n Test 6 (patchItem) passed \n");
	}

	@Test
	void patchItemWithTagsIgnoreS3UpdateMetadata_FilePresentInS3() {

		log.warn("DocumentInternalApiControllerTest.patchItemWithTagsIgnoreS3UpdateMetadata_FilePresentInS3() : START");

		//The fileKey is in ignored-update-metadata.csv file
		String documentKey = "fileKeyToIgnoreUpdateMetadata2Tags";

		DocTypeEntity docTypeEntity = getTypeEntity();

		var documentEntity = new DocumentEntity();
		documentEntity.setDocumentKey(documentKey);
		documentEntity.setDocumentType(docTypeEntity);
		documentEntity.setContentLenght(new BigDecimal(50));
		documentEntity.setDocumentState(PRELOADED);
		documentEntity.setDocumentLogicalState(AVAILABLE);
		documentEntity.setTags(createTagsList());

		insertDocumentEntity(documentEntity);
		addFileToBucket(documentKey, bucketName.ssHotName());

		DocumentChanges docChanges = new DocumentChanges();
		docChanges.setDocumentState(ATTACHED);
		docChanges.setContentLenght(new BigDecimal(60));
		docChanges.setLastStatusChangeTimestamp(OffsetDateTime.now().minusMinutes(10));

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

		verify(s3Service, times(1)).putObjectTagging(anyString(), anyString(), any());
		verify(retentionService, times(1)).setRetentionPeriodInBucketObjectMetadata(anyString(), anyString(), any(), any(), anyString());

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
	void patchItemWithTagsIdempotenceOk() {

		DocumentChanges docChanges = new DocumentChanges();
		docChanges.setDocumentState(SAVED);
		docChanges.setContentLenght(new BigDecimal(50));

		webTestClient.patch().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_ENTITY_TAGS))
				.accept(APPLICATION_JSON).contentType(APPLICATION_JSON).body(BodyInserters.fromValue(docChanges))
				.exchange().expectStatus().isEqualTo(HttpStatus.OK);
		log.info("\n Test 6 (patchItem) passed \n");
	}

	@Test
	void patchItemLastStatusChangeOk() {

		DocumentChanges docChanges = new DocumentChanges();
		docChanges.setDocumentState(AVAILABLE);
		docChanges.setContentLenght(new BigDecimal(60));
		docChanges.setLastStatusChangeTimestamp(OffsetDateTime.now().minusMinutes(10));

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
	void patchItemIncorrectParameters() {

		webTestClient.patch().uri(BASE_PATH).accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
				.body(BodyInserters.fromValue(documentInput)).exchange().expectStatus()
				.isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);

		log.info("\n Test 8 (patchItemIncorrectParameters) passed \n");

	}

	@Test
	void patchItemDocumentDeletedKo() {
		String documentKey = "docKeyDeletedDocument";

		DocTypeEntity docTypeEntity = new DocTypeEntity();
		docTypeEntity.setTipoDocumento(DOCTYPE_ID_LEGAL_FACTS);

		var documentEntity = new DocumentEntity();
		documentEntity.setDocumentKey(documentKey);
		documentEntity.setDocumentType(docTypeEntity);
		documentEntity.setContentLenght(new BigDecimal(50));
		documentEntity.setDocumentState(DELETED);

		insertDocumentEntity(documentEntity);

		DocumentChanges docChanges = new DocumentChanges();
		docChanges.setDocumentState(SAVED);
		docChanges.setContentLenght(new BigDecimal(50));

		webTestClient.patch().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(documentKey))
				.accept(APPLICATION_JSON).contentType(APPLICATION_JSON).body(BodyInserters.fromValue(docChanges))
				.exchange().expectStatus().isEqualTo(HttpStatus.GONE);
		log.info("\n Test 6 (patchItem) passed \n");
	}

	@Test
	// codice test: DCSS.103.1
	void deleteItem() {

		webTestClient.delete().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT))
				.accept(APPLICATION_JSON).exchange().expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

		log.info("\n Test 9 (deleteItem) passed \n");

	}

	@Test
		// codice test: DCSS.103.1
	void deleteItemWithTags() {

		webTestClient.delete().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT_TAGS_DELETE))
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
		byte[] fileBytes = readPdfDocument();
		PutObjectRequest request = PutObjectRequest.builder()
				.bucket(bucketName)
				.key(fileName)
				.contentMD5(new String(Base64.encodeBase64(DigestUtils.md5(fileBytes)))).build();
		s3TestClient.putObject(request, RequestBody.fromBytes(fileBytes));
	}

    private byte[] readPdfDocument() {
        byte[] byteArray=null;
        try {
            ByteArrayOutputStream buffer;
            try (InputStream is = getClass().getResourceAsStream("/PDF_PROVA.pdf")) {
                buffer = new ByteArrayOutputStream();

                int nRead;
                byte[] data = new byte[16384];

                while ((nRead = Objects.requireNonNull(is).read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
            }

            byteArray = buffer.toByteArray();

        } catch (FileNotFoundException e) {
        	log.debug("File Not found",e);
        } catch (IOException e) {
            log.debug("IO Ex", e);
        }
        return byteArray;

    }


	@ParameterizedTest
	@MethodSource("provideThrowable")
	void testGetResponse(Map.Entry<Throwable, HttpStatus> entry) {
		Throwable throwable = entry.getKey();
		HttpStatus expectedStatus = entry.getValue();

		Mono<ResponseEntity<DocumentResponse>> responseMono =
				ReflectionTestUtils.invokeMethod(documentInternalApiController, "getResponse", null, throwable);

		Assertions.assertNotNull(responseMono);
		ResponseEntity<DocumentResponse> response = responseMono.block();
		Assertions.assertNotNull(response);
		Assertions.assertEquals(expectedStatus, response.getStatusCode());
		Assertions.assertNotNull(response.getBody());
		Assertions.assertNotNull(response.getBody().getError());
	}

	private static Stream<Map.Entry<Throwable, HttpStatus>> provideThrowable() {
		Map<Throwable, HttpStatus> throwableStatusMap = Map.of(
				new ItemAlreadyPresent(""), HttpStatus.CONFLICT,
				new DocumentKeyNotPresentException(""), HttpStatus.NOT_FOUND,
				new RepositoryManagerException(), HttpStatus.BAD_REQUEST,
				new IllegalDocumentStateException(""), HttpStatus.BAD_REQUEST,
				new DocumentTypeNotPresentException(""), HttpStatus.BAD_REQUEST,
				new InvalidNextStatusException("", ""), HttpStatus.BAD_REQUEST,
				NoSuchKeyException.builder().build(), HttpStatus.BAD_REQUEST,
				new RetentionException(""), HttpStatus.BAD_REQUEST,
				new DateTimeException(""), HttpStatus.INTERNAL_SERVER_ERROR,
				new RuntimeException(""), HttpStatus.INTERNAL_SERVER_ERROR
		);

		return throwableStatusMap.entrySet().stream();
	}

}
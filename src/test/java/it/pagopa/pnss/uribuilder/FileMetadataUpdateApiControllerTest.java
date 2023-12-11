package it.pagopa.pnss.uribuilder;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.model.dto.MacchinaStatiValidateStatoResponseDto;
import it.pagopa.pnss.common.model.pojo.DocumentStatusChange;
import it.pagopa.pnss.common.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.DocTypeEntity;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.repositorymanager.service.DocumentService;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
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
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;

import java.sql.Date;
import java.time.Instant;

import static it.pagopa.pnss.common.DocTypesConstant.PN_AAR;
import static it.pagopa.pnss.common.DocTypesConstant.PN_NOTIFICATION_ATTACHMENTS;
import static it.pagopa.pnss.common.UserConfigurationConstant.*;
import static it.pagopa.pnss.common.constant.Constant.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient(timeout = "36000")
@CustomLog
class FileMetadataUpdateApiControllerTest {

	@Autowired
	private WebTestClient webClient;
	@MockBean
	private CallMacchinaStati callMacchinaStati;
	@Autowired
	private S3Client s3TestClient;
	@Autowired
	private BucketName bucketName;
	@Autowired
	private DocumentService documentService;
	@Autowired
	private S3Service s3Service;
	@Value("${header.x-api-key:#{null}}")
	private String xApiKey;
	@Value("${header.x-pagopa-safestorage-cx-id:#{null}}")
	private String X_PAGOPA_SAFESTORAGE_CX_ID;
	@Value("${file.updateMetadata.api.url}")
	private String urlPath;
	private static final String DOCUMENT_KEY = "DOCUMENT_KEY";
	private static final String RETENTION_UNTIL = "2023-09-07T17:34:15+02:00";
	private static DynamoDbTable<DocumentEntity> documentEntityDynamoDbTable;
	private static DynamoDbTable<DocTypeEntity> docTypeEntityDynamoDbTable;

	private WebTestClient.ResponseSpec fileMetadataUpdateTestCall(UpdateFileMetadataRequest updateFileMetadataRequest, String documentKey, String clientId, String apiKey) {
		return webClient.post()
				.uri(uriBuilder -> uriBuilder.path(urlPath).queryParam("metadataOnly", false).build(documentKey))
				.header(X_PAGOPA_SAFESTORAGE_CX_ID, clientId)
				.header(xApiKey, apiKey)
				.header(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE)
				.bodyValue(updateFileMetadataRequest)
				.exchange();
	}

	@BeforeAll
	public static void init(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient, @Autowired RepositoryManagerDynamoTableName gestoreRepositoryDynamoDbTableName) {
		documentEntityDynamoDbTable = dynamoDbEnhancedClient.table(gestoreRepositoryDynamoDbTableName.documentiName(), TableSchema.fromBean(DocumentEntity.class));
		docTypeEntityDynamoDbTable = dynamoDbEnhancedClient.table(gestoreRepositoryDynamoDbTableName.tipologieDocumentiName(), TableSchema.fromBean(DocTypeEntity.class));
	}

	@BeforeEach
	void beforeEach() {
		MacchinaStatiValidateStatoResponseDto macchinaStatiValidateStatoResponseDto = new MacchinaStatiValidateStatoResponseDto();
		macchinaStatiValidateStatoResponseDto.setAllowed(true);
		when(callMacchinaStati.statusValidation(any(DocumentStatusChange.class))).thenReturn(Mono.just(macchinaStatiValidateStatoResponseDto));
	}

	@Test
	void testFileMetadataUpdateOk() {
		//setup
		documentService.insertDocument(new DocumentInput().documentKey(DOCUMENT_KEY).documentState(PRELOADED).retentionUntil(RETENTION_UNTIL).documentType(PN_NOTIFICATION_ATTACHMENTS)).block();
		s3Service.putObject(DOCUMENT_KEY, new byte[10], bucketName.ssHotName()).block();
		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().status(ATTACHED), DOCUMENT_KEY, PN_TEST, PN_TEST_API_KEY)
				.expectStatus()
				.isOk();
		//clean-up
		documentService.deleteDocument(DOCUMENT_KEY).block();
		s3Service.deleteObject(DOCUMENT_KEY, bucketName.ssHotName()).block();
	}
	@Test
	void testMetadataUpdateDocumentKeyNotPresent() {
		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest(), "MISSING_DOC_KEY", PN_TEST, PN_TEST_API_KEY)
				.expectStatus()
				.isNotFound();
	}

	@Test
	void testMetadataUpdateInvalidStatus() {
		documentService.insertDocument(new DocumentInput().documentKey(DOCUMENT_KEY).retentionUntil(RETENTION_UNTIL).documentType(PN_AAR)).block();
		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().status(PRELOADED), DOCUMENT_KEY, PN_TEST, PN_TEST_API_KEY)
				.expectStatus()
				.isBadRequest();
		documentService.deleteDocument(DOCUMENT_KEY).block();
	}

	@Test
	void testUpdateMetadataUnauthorizedClientId() {
		documentService.insertDocument(new DocumentInput().documentKey(DOCUMENT_KEY).retentionUntil(RETENTION_UNTIL).documentType(PN_AAR)).block();
		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().status(SAVED), DOCUMENT_KEY, PN_DELIVERY, PN_DELIVERY_API_KEY)
				.expectStatus()
				.isForbidden();
		documentService.deleteDocument(DOCUMENT_KEY).block();
	}

	@Test
	void testUpdateMetadataRetentionUntilOk() {
		documentService.insertDocument(new DocumentInput().documentKey(DOCUMENT_KEY).documentType(PN_AAR)).block();
		putObjectInBucket(DOCUMENT_KEY);
		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().retentionUntil(Date.from(Instant.parse(RETENTION_UNTIL))), DOCUMENT_KEY, PN_TEST, PN_TEST_API_KEY)
				.expectStatus()
				.isOk();
		documentService.deleteDocument(DOCUMENT_KEY).block();
	}

	private void putObjectInBucket(String fileName) {
		byte[] fileBytes = new byte[10];
		PutObjectRequest request = PutObjectRequest.builder().bucket(bucketName.ssHotName()).key(fileName).contentMD5(new String(Base64.encodeBase64(DigestUtils.md5(fileBytes)))).build();
		s3TestClient.putObject(request, RequestBody.fromBytes(fileBytes));
	}

}

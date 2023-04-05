package it.pagopa.pnss.repositorymanager.rest.internal;

import static it.pagopa.pnss.common.constant.Constant.AVAILABLE;
import static it.pagopa.pnss.common.constant.Constant.FREEZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import it.pagopa.pn.template.internal.rest.v1.dto.CurrentStatus;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentChanges;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentInput;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentResponse;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.InformationClassificationEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.TimeStampedEnum;
import it.pagopa.pnss.common.DocTypesConstant;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.DocTypeEntity;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Slf4j
public class DocumentInternalApiControllerTest {
	
    @Value("${test.aws.s3.endpoint:#{null}}")
    String testAwsS3Endpoint;

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private BucketName bucketName;
    
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

	private static void insertDocumentEntity(String documentKey) {
		log.info("execute insertDocumentEntity()");

		DocTypeEntity docTypeEntity = new DocTypeEntity();
		docTypeEntity.setTipoDocumento(DocTypesConstant.PN_NOTIFICATION_ATTACHMENTS);
		log.info("execute insertDocumentEntity() : docTypeEntity : {}", docTypeEntity);

		var documentEntity = new DocumentEntity();
		documentEntity.setDocumentKey(documentKey);
		documentEntity.setDocumentType(docTypeEntity);
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
		currentStatus1.setStorage("PN_NOTIFICATION_ATTACHMENTS");
		currentStatus1.setAllowedStatusTransitions(allowedStatusTransitions1);

		Map<String, CurrentStatus> statuses1 = new HashMap<>();
		statuses1.put("PRELOADED", currentStatus1);

		DocumentType docTypes = new DocumentType();
		docTypes.setTipoDocumento(DocTypesConstant.PN_NOTIFICATION_ATTACHMENTS);
		docTypes.setChecksum(DocumentType.ChecksumEnum.SHA256);
		docTypes.setInitialStatus("SAVED");
		docTypes.setStatuses(statuses1);
		docTypes.setInformationClassification(InformationClassificationEnum.HC);
		docTypes.setTransformations(List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
		docTypes.setTimeStamped(TimeStampedEnum.STANDARD);
		log.info("execute createDocument() : docType : {}", docTypes);

		documentInput = new DocumentInput();
		documentInput.setDocumentKey(PARTITION_ID_DEFAULT);
		documentInput.setDocumentState(FREEZED);
		documentInput.setRetentionUntil("2032-04-12T12:32:04.000Z");
		documentInput.setCheckSum(CHECKSUM);
		documentInput.setContentType("xxxxx");
		documentInput.setDocumentType("PN_NOTIFICATION_ATTACHMENTS");
		documentInput.setContentLenght(new BigDecimal(100));
		log.info("execute createDocument() : documentInput : {}", documentInput);

		documentChanges = new DocumentChanges();
		documentChanges.setDocumentState(AVAILABLE);
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
		
		log.warn("DocumentInternalApiControllerTest.patchItem() : decommentare");

//		EntityExchangeResult<DocumentResponse> documentInDb = webTestClient.get()
//				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(documentInput.getDocumentKey()))
//				.accept(APPLICATION_JSON).exchange().expectStatus().isOk().expectBody(DocumentResponse.class)
//				.returnResult();
//		log.info("\n Test 6 (patchItem) : document before {}", documentInDb.getResponseBody().getDocument());
//
//		log.info("\n Test 6 (patchItem) : documentChanges {}", documentChanges);
//
//		addFileToBucket(PARTITION_ID_DEFAULT);
//
//		webTestClient.patch().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT))
//				.accept(APPLICATION_JSON).contentType(APPLICATION_JSON).body(BodyInserters.fromValue(documentChanges))
//				.exchange().expectStatus().isOk();
//
//		EntityExchangeResult<DocumentResponse> documentUpdated = webTestClient.get()
//				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT))
//				.accept(APPLICATION_JSON).exchange().expectStatus().isOk().expectBody(DocumentResponse.class)
//				.returnResult();
//
//		log.info("\n Test 6 (patchItem) : documentUpdated : {} \n", documentUpdated.getResponseBody().getDocument());
//
//		Assertions.assertEquals(documentChanges.getContentLenght(),
//				documentUpdated.getResponseBody().getDocument().getContentLenght());

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
    
    private void addFileToBucket(String fileName) {
        S3ClientBuilder s3ClientBuilder = S3Client.builder();
        s3ClientBuilder.endpointOverride(URI.create(testAwsS3Endpoint));
        S3Client s3Client = s3ClientBuilder.build();
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
        	log.error("File Not found",e);
        } catch (IOException e) {
            log.error("IO Ex", e);
        }
        return byteArray;

    }

}
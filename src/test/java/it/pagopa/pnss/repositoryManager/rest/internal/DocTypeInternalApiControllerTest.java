package it.pagopa.pnss.repositoryManager.rest.internal;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.ChecksumEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.InformationClassificationEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.TimeStampedEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.TipoDocumentoEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentTypeResponse;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositoryManager.constant.DynamoTableNameConstant;
import it.pagopa.pnss.repositoryManager.entity.DocTypeEntity;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Slf4j
public class DocTypeInternalApiControllerTest {

	@Autowired
	private WebTestClient webTestClient;
	@Autowired
	private RepositoryManagerDynamoTableName repositoryManagerDynamoTableName;

	private static final String BASE_PATH = "/safestorage/internal/v1/doctypes";
	private static final String BASE_PATH_WITH_PARAM = String.format("%s/{typeId}", BASE_PATH);

	private static final String PARTITION_ID_DEFAULT_NOTIFICATION_ATTACHMENTS = TipoDocumentoEnum.NOTIFICATION_ATTACHMENTS.getValue();
	private static final TipoDocumentoEnum PARTITION_ID_INSERT_LEGAL_FACTS = TipoDocumentoEnum.LEGAL_FACTS;
	private static final String PARTITION_ID_NO_EXISTENT_AAR = TipoDocumentoEnum.AAR.getValue();
	
	private static DocumentType docTypesInsertInput;
	private static DocumentType docTypesUpdateDeleteInput;
	
	private static DynamoDbTable<DocTypeEntity> dynamoDbTable;
	
    private static void insertDocTypeEntity(String tipoDocumento) {
    	log.info("execute insertDocTypeEntity()");
        var docTypeEntity = new DocTypeEntity();
        docTypeEntity.setTipoDocumento(TipoDocumentoEnum.fromValue(tipoDocumento));
        dynamoDbTable.putItem(builder -> builder.item(docTypeEntity));
    }
	
    @BeforeAll
    public static void insertDefaultDocType(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient,
    		@Autowired RepositoryManagerDynamoTableName gestoreRepositoryDynamoDbTableName) 
    {
    	log.info("execute insertDefaultDocType()");
        dynamoDbTable = dynamoDbEnhancedClient.table(
//        		DynamoTableNameConstant.DOC_TYPES_TABLE_NAME, 
        		gestoreRepositoryDynamoDbTableName.tipologieDocumentiName(),
        		TableSchema.fromBean(DocTypeEntity.class));
        insertDocTypeEntity(PARTITION_ID_DEFAULT_NOTIFICATION_ATTACHMENTS);
    }
	
    @BeforeEach
	public void createDocumentType() {
    	log.info("execute createDocumentType() : START");
    	
    	docTypesInsertInput = new DocumentType();
    	docTypesInsertInput.setTipoDocumento(PARTITION_ID_INSERT_LEGAL_FACTS);
    	docTypesInsertInput.setChecksum(ChecksumEnum.MD5);
    	docTypesInsertInput.setLifeCycleTag("lifeCicle1");
    	docTypesInsertInput.setInformationClassification(InformationClassificationEnum.C);
    	docTypesInsertInput.setDigitalSignature(true);
    	docTypesInsertInput.setTimeStamped(TimeStampedEnum.STANDARD);
		log.info("execute createDocumentType() : docTypesInsertInput : {}", docTypesInsertInput);
    	
    	docTypesUpdateDeleteInput = new DocumentType();
		docTypesUpdateDeleteInput.setTipoDocumento(TipoDocumentoEnum.fromValue(PARTITION_ID_DEFAULT_NOTIFICATION_ATTACHMENTS));
		docTypesUpdateDeleteInput.setChecksum(ChecksumEnum.MD5);
		docTypesUpdateDeleteInput.setLifeCycleTag("lifeCicle1");
		docTypesUpdateDeleteInput.setInformationClassification(InformationClassificationEnum.C);
		docTypesUpdateDeleteInput.setDigitalSignature(true);
		docTypesUpdateDeleteInput.setTimeStamped(TimeStampedEnum.STANDARD);
		log.info("execute createDocumentType() : docTypesUpdateDeleteInput : {}", docTypesUpdateDeleteInput);
	}

	@Test
	// Codice test: DTSS.101.1
	void postItem() {
		
		EntityExchangeResult<DocumentTypeResponse> resultPreInsert =
				webTestClient.get()
					.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(docTypesInsertInput.getTipoDocumento().getValue()))
					.accept(APPLICATION_JSON)
					.exchange()
					.expectBody(DocumentTypeResponse.class).returnResult();
		
		log.info("\n Test 1 (postItem) resultPreInsert {} \n", resultPreInsert);

		if (resultPreInsert == null || resultPreInsert.getResponseBody() == null || resultPreInsert.getResponseBody().getDocType() == null) 
		{
			webTestClient.post()
						 .uri(BASE_PATH)
						 .accept(APPLICATION_JSON)
						 .contentType(APPLICATION_JSON)
						 .body(BodyInserters.fromValue(docTypesInsertInput))
						 .exchange()
						 .expectStatus().isOk();
			
		}
		
		log.info("\n Test 1 (postItem) passed \n");

	}
	
	@Test
	// Codice test: DTSS.101.2
	void postItemIncorrectParameters() {

		docTypesInsertInput.setTipoDocumento(null);
		
		webTestClient.post()
					 .uri(BASE_PATH)
					 .accept(APPLICATION_JSON)
					 .contentType(APPLICATION_JSON)
					 .body(BodyInserters.fromValue(docTypesInsertInput))
					 .exchange()
					 .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);

		log.info("\n Test 2 (postItemIncorrectParameters) passed \n");

	}
	
	@Test
	void postItemDuplicatedKey() {
		
		EntityExchangeResult<DocumentTypeResponse> resultPreInsert =
				webTestClient.get()
					.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(docTypesInsertInput.getTipoDocumento().getValue()))
					.accept(APPLICATION_JSON)
					.exchange()
					.expectBody(DocumentTypeResponse.class).returnResult();

		if (resultPreInsert != null && resultPreInsert.getResponseBody() != null && resultPreInsert.getResponseBody().getDocType() != null) 
		{
			webTestClient.post()
						 .uri(BASE_PATH)
						 .accept(APPLICATION_JSON)
						 .contentType(APPLICATION_JSON)
						 .body(BodyInserters.fromValue(docTypesInsertInput))
						 .exchange()
			        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
		}

		log.info("\n Test 2 (postItemDuplicatedKey) passed \n");

	}

	@Test
	// codice test: DTSS.100.1
	void getItem() {

		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT_NOTIFICATION_ATTACHMENTS))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectBody(DocumentTypeResponse.class);

		log.info("\n Test 3 (getItem) passed \n");

	}
	
	@Test
	// codice test: DTSS.100.2
	void getItemNoExistentKey() {
		
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_NO_EXISTENT_AAR))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
		
		log.info("\n Test 4 (getItemNoExistentKey) passed \n");

	}
	
	@Test
	// codice test: DTSS.100.3
	void getItemIncorrectParameter() {

		webTestClient.get()
			.uri(BASE_PATH)
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);

		log.info("\n Test 5 (getItemIncorrectParameters) passed \n");

	}

	@Test
	// codice test: DTSS.102.1
	void putItem() {
		
		webTestClient.put()
			         .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT_NOTIFICATION_ATTACHMENTS))
			         .accept(APPLICATION_JSON)
			         .contentType(APPLICATION_JSON)
			         .body(BodyInserters.fromValue(docTypesUpdateDeleteInput))
			         .exchange()
			         .expectStatus().isOk();

		log.info("\n Test 6 (putItem) passed \n");

	}
	
	@Test
	// codice test: DTSS.102.2
	void putItemNoExistentKey() {
		
		webTestClient.put()
			         .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_NO_EXISTENT_AAR))
			         .accept(APPLICATION_JSON)
			         .contentType(APPLICATION_JSON)
			         .body(BodyInserters.fromValue(docTypesUpdateDeleteInput))
			         .exchange()
			         .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);

		log.info("\n Test 7 (putItemNoExistentKey) passed \n");

	}
	
	@Test
	// codice test: DTSS.102.3
	void putItemIncorretctParameter() {
		
		webTestClient.put()
			         .uri(BASE_PATH)
			         .accept(APPLICATION_JSON)
			         .contentType(APPLICATION_JSON)
			         .body(BodyInserters.fromValue(docTypesUpdateDeleteInput))
			         .exchange()
			         .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);

		log.info("\n Test 8 (putItemIncorretctParameters) passed \n");

	}

	@Test
	// codice test: DTSS.103.1
	void deleteItem() {
		
		EntityExchangeResult<DocumentTypeResponse> result =
			webTestClient.delete()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT_NOTIFICATION_ATTACHMENTS))
		        .accept(APPLICATION_JSON)
		        .exchange()
		        .expectStatus().isOk()
		        .expectBody(DocumentTypeResponse.class).returnResult();
		
		Assertions.assertEquals(PARTITION_ID_DEFAULT_NOTIFICATION_ATTACHMENTS, result.getResponseBody().getDocType().getTipoDocumento().getValue());
	    
	    log.info("\n Test 9 (deleteItem) passed \n");

	}
	
	@Test
	// codice test: DTSS.103.2
	void deleteItemNoExistentKey() {
		
		webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_NO_EXISTENT_AAR))
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
	    
	    log.info("\n Test 10 (deleteItemNoExistentKey) passed \n");

	}
	
	@Test
	// codice test: DTSS.103.3
	void deleteItemIncorrectParameters() {
		
		webTestClient.delete()
			.uri(BASE_PATH)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
	    
	    log.info("\n Test 11 (deleteItemIncorrectParameters) passed \n");

	}

}

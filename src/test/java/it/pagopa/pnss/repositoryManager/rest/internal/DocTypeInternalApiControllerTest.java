package it.pagopa.pnss.repositoryManager.rest.internal;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import it.pagopa.pnss.repositoryManager.constant.DynamoTableNameConstant;
import it.pagopa.pnss.repositoryManager.entity.DocTypeEntity;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class DocTypeInternalApiControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	private static final String BASE_PATH = "/safestorage/internal/v1/doctypes";
	private static final String BASE_PATH_WITH_PARAM = String.format("%s/{typeId}", BASE_PATH);

	private static final it.pagopa.pnss.common.client.enumeration.TipoDocumentoEnum PARTITION_ID_ENTITY 
		= it.pagopa.pnss.common.client.enumeration.TipoDocumentoEnum.NOTIFICATION_ATTACHMENTS;
	
	private static final String PARTITION_ID_DEFAULT = PARTITION_ID_ENTITY.getValue();
	private static final String PARTITION_ID_NO_EXISTENT = TipoDocumentoEnum.EXTERNAL_LEGAL_FACTS.getValue();
	
	private static DocumentType docTypesInput;
	
	private static DynamoDbTable<DocTypeEntity> dynamoDbTable;
	
    private static void insertDocTypeEntity(it.pagopa.pnss.common.client.enumeration.TipoDocumentoEnum tipoDocumento) {
    	log.info("execute insertDocTypeEntity()");
        var docTypeEntity = new DocTypeEntity();
        docTypeEntity.setTipoDocumento(tipoDocumento);
        dynamoDbTable.putItem(builder -> builder.item(docTypeEntity));
    }
	
    @BeforeAll
    public static void insertDefaultDocType(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient) {
    	log.info("execute insertDefaultDocType()");
        dynamoDbTable = dynamoDbEnhancedClient.table(DynamoTableNameConstant.DOC_TYPES_TABLE_NAME, TableSchema.fromBean(DocTypeEntity.class));
        insertDocTypeEntity(PARTITION_ID_ENTITY);
    }
	
    @BeforeEach
	public void createDocumentType() {
    	log.info("execute createDocumentType()");
    	docTypesInput = new DocumentType();
		docTypesInput.setTipoDocumento(TipoDocumentoEnum.fromValue(PARTITION_ID_DEFAULT));
		docTypesInput.setChecksum(ChecksumEnum.MD5);
		docTypesInput.setLifeCycleTag("lifeCicle1");
		docTypesInput.setInformationClassification(InformationClassificationEnum.C);
		docTypesInput.setDigitalSignature(true);
		docTypesInput.setTimeStamped(TimeStampedEnum.STANDARD);
	}

	@Test
	// Codice test: DTSS.101.1
	void postItem() {

		docTypesInput.setTipoDocumento(TipoDocumentoEnum.LEGAL_FACTS);
		
		webTestClient.post()
					 .uri(BASE_PATH)
					 .accept(APPLICATION_JSON)
					 .contentType(APPLICATION_JSON)
					 .body(BodyInserters.fromValue(docTypesInput))
					 .exchange()
					 .expectStatus().isOk();

		log.info("\n Test 1 (postItem) passed \n");

	}
	
	@Test
	// Codice test: DTSS.101.2
	void postItemIncorrectParameter() {

		docTypesInput.setTipoDocumento(null);
		
		webTestClient.post()
					 .uri(BASE_PATH)
					 .accept(APPLICATION_JSON)
					 .contentType(APPLICATION_JSON)
					 .body(BodyInserters.fromValue(docTypesInput))
					 .exchange()
		        .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);

		log.info("\n Test 2 (postItemIncorrectParameters) passed \n");

	}
	
	@Test
	void postItemDuplicatedKey() {

		webTestClient.post()
					 .uri(BASE_PATH)
					 .accept(APPLICATION_JSON)
					 .contentType(APPLICATION_JSON)
					 .body(BodyInserters.fromValue(docTypesInput))
					 .exchange()
		        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);

		log.info("\n Test 2 (postItemDuplicatedKey) passed \n");

	}

	@Test
	// codice test: DTSS.100.1
	void getItem() {

		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT))
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectBody(DocumentType.class);

		log.info("\n Test 3 (getItem) passed \n");

	}
	
	@Test
	// codice test: DTSS.100.2
	void getItemNoExistentKey() {
		
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_NO_EXISTENT))
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
			         .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT))
			         .accept(APPLICATION_JSON)
			         .contentType(APPLICATION_JSON)
			         .body(BodyInserters.fromValue(docTypesInput))
			         .exchange()
			         .expectStatus().isOk();

		log.info("\n Test 6 (putItem) passed \n");

	}
	
	@Test
	// codice test: DTSS.102.2
	void putItemNoExistentKey() {
		
		webTestClient.put()
			         .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_NO_EXISTENT))
			         .accept(APPLICATION_JSON)
			         .contentType(APPLICATION_JSON)
			         .body(BodyInserters.fromValue(docTypesInput))
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
			         .body(BodyInserters.fromValue(docTypesInput))
			         .exchange()
			         .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);

		log.info("\n Test 8 (putItemIncorretctParameters) passed \n");

	}

	@Test
	// codice test: DTSS.103.1
	void deleteItem() {
		
		EntityExchangeResult<DocumentType> result =
			webTestClient.delete()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT))
		        .accept(APPLICATION_JSON)
		        .exchange()
		        .expectStatus().isOk()
		        .expectBody(DocumentType.class).returnResult();
		
		Assertions.assertEquals(PARTITION_ID_DEFAULT, result.getResponseBody().getTipoDocumento().getValue());
	    
	    log.info("\n Test 9 (deleteItem) passed \n");

	}
	
	@Test
	// codice test: DTSS.103.2
	void deleteItemNoExistentKey() {
		
		webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_NO_EXISTENT))
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
	    
	    log.info("\n Test 10 (deleteItemNoExistentKey) passed \n");

	}
	
	@Test
	// codice test: DTSS.103.3
	void deleteItemIncorrectParameter() {
		
		webTestClient.delete()
			.uri(BASE_PATH)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
	    
	    log.info("\n Test 11 (deleteItemIncorrectParameters) passed \n");

	}

}

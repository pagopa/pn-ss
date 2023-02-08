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

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.Document.CheckSumEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.Document.DocumentStateEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.Document.DocumentTypeEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentResponse;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositoryManager.entity.DocumentEntity;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Slf4j
public class DocumentInternalApiControllerTest {
	
	@Autowired
	private WebTestClient webTestClient;

	private static final String BASE_PATH = "/safestorage/internal/v1/documents";
	private static final String BASE_PATH_WITH_PARAM = String.format("%s/{documentKey}", BASE_PATH);

	private static final String PARTITION_ID_ENTITY = "documentKeyEnt";
	private static final String PARTITION_ID_DEFAULT = PARTITION_ID_ENTITY;
	private static final String PARTITION_ID_NO_EXISTENT = "documentKey_bad";
	
	private static Document documentInput;
	
	private static DynamoDbTable<DocumentEntity> dynamoDbTable;
	
    private static void insertDocumentEntity(String documentKey) {
    	log.info("execute insertDocumentEntity()");
        var documentEntity = new DocumentEntity();
        documentEntity.setDocumentKey(documentKey);
        dynamoDbTable.putItem(builder -> builder.item(documentEntity));
    }
    
    @BeforeAll
    public static void insertDefaultDocument(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient,
    		@Autowired RepositoryManagerDynamoTableName gestoreRepositoryDynamoDbTableName) 
    {
    	log.info("execute insertDefaultDocument()");
    	dynamoDbTable = dynamoDbEnhancedClient.table(
//    			DynamoTableNameConstant.DOCUMENT_TABLE_NAME, 
    			gestoreRepositoryDynamoDbTableName.documentiName(),
    			TableSchema.fromBean(DocumentEntity.class));
    	insertDocumentEntity(PARTITION_ID_ENTITY);
    }
	
    @BeforeEach
	public void createDocument() {
    	log.info("execute createDocument()");
    	documentInput = new Document();
    	documentInput.setDocumentKey(PARTITION_ID_DEFAULT);
    	documentInput.setDocumentState(DocumentStateEnum.FREEZED);
    	documentInput.setRetentionUntil("2032-04-12T12:32:04.000Z");
    	documentInput.setCheckSum(CheckSumEnum.MD5);
		documentInput.setContentType("xxxxx");
		documentInput.setDocumentType(DocumentTypeEnum.NOTIFICATION_ATTACHMENTS);
	}
    	
    @Test
    // codice test: DCSS.101.1
    void postItem() {
    	
    	documentInput.setDocumentKey("document_key_one");
    	
 		webTestClient.post()
	        .uri(BASE_PATH)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(documentInput))
	        .exchange()
	        .expectStatus().isOk();
		
		log.info("\n Test 1 (postItem) passed \n");
    	
    }
    
    @Test
    // codice test: DCSS.101.2
    void postItemPartitionKeyDuplicated() {
  
		webTestClient.post()
	        .uri(BASE_PATH)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(documentInput))
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
			
		log.info("\n Test 2 (postItemPartitionKeyDuplicated) passed \n");
    	
    }
    
    @Test
    // codice test: DCSS.101.2
    void postItemIncorrectParameter() {
    	
    	documentInput.setDocumentKey(null);
  
		webTestClient.post()
	        .uri(BASE_PATH)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(documentInput))
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
			
		log.info("\n Test 2 (postItemIncorrectParameter) passed \n");
    	
    }
    
    @Test
    // codice test: DCSS.100.1
    void getItem() {
    	
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT))
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isOk()
	        .expectBody(DocumentResponse.class);
	    
	    log.info("\n Test 3 (getItem) passed \n");
  
    }
    
    @Test
    // codice test: DCSS.100.2
    void getItemNoExistentPartitionKey() {
    	
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_NO_EXISTENT))
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
	    
	    log.info("\n Test 4 (getItemNoExistentPartitionKey) passed \n");
  
    }
    
    @Test
    // codice test: DCSS.100.3
    public void getItemIncorrectParameters() {
    	
		webTestClient.get()
			.uri(BASE_PATH)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
	    
	    log.info("\n Test 5 (getItemIncorrectParameters) passed \n");
  
    }
	
    @Test
    // codice test: DCSS.102.1
    void patchItem() {
    	
    	log.info("patchItem() : {}", documentInput);
    	documentInput.setDocumentState(DocumentStateEnum.BOOKED);
    	log.info("patchItem() : change state : {}", documentInput);
  
		webTestClient.patch()
	        .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT))
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(documentInput))
	        .exchange()
	        .expectStatus().isOk();
		
		EntityExchangeResult<DocumentResponse> documentUpdated = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT))
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isOk()
	        .expectBody(DocumentResponse.class).returnResult();
		
		log.info("\n Test 6 (patchItem) documentUpdated : {} \n", documentUpdated.getResponseBody().getDocument());
		
//		Assertions.assertEquals(documentInput.getDocumentState(), documentUpdated.getResponseBody().getDocumentState());
	
		log.info("\n Test 6 (patchItem) passed \n");
    	
    }
    
    @Test
    // codice test: DCSS.102.2
    void patchItemNoExistentKey() {
    	
		webTestClient.patch()
	        .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_NO_EXISTENT))
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(documentInput))
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
	
		log.info("\n Test 7 (patchItemNoExistentKey) passed \n");
    	
    }
    
    @Test
    // codice test: DCSS.102.3
    void patchItemIncorretcParameters() {
  
		webTestClient.patch()
	        .uri(BASE_PATH)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(documentInput))
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
	
		log.info("\n Test 8 (patchItemIncorretcParameters) passed \n");
    	
    }
    
    @Test
    // codice test: DCSS.103.1
    void deleteItem() {
    	
    	EntityExchangeResult<DocumentResponse> result =
			webTestClient.delete()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT))
		        .accept(APPLICATION_JSON)
		        .exchange()
		        .expectStatus().isOk()
		        .expectBody(DocumentResponse.class).returnResult();
			
		Assertions.assertEquals(PARTITION_ID_DEFAULT, result.getResponseBody().getDocument().getDocumentKey());
	    
	    log.info("\n Test 9 (deleteItem) passed \n");
  
    }
    
    @Test
    // codice test: DCSS.103.2
    void deleteItemNoExistentKey() {
    	
		webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_NO_EXISTENT))
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
	    
	    log.info("\n Test 10 (deleteItemNoExistentKey) passed \n");
  
    }
    
    @Test
    // codice test: DCSS.103.3
    void deleteItemIncorrectParameter() {
    	
		webTestClient.delete()
			.uri(BASE_PATH)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
	    
	    log.info("\n Test 10 (deleteItemIncorrectParameter) passed \n");
  
    }
    
}
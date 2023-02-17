package it.pagopa.pnss.repositorymanager.rest.internal;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import it.pagopa.pn.template.internal.rest.v1.dto.CurrentStatus;
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.Document.CheckSumEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.Document.DocumentStateEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentChanges;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.ChecksumEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.InformationClassificationEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.TimeStampedEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentResponse;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
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
	
	private static final String DOCTYPE_ID_LEGAL_FACTS = "PN_LEGAL_FACTS";

	private static final String PARTITION_ID_ENTITY = "documentKeyEnt";
	private static final String PARTITION_ID_DEFAULT = PARTITION_ID_ENTITY;
	private static final String PARTITION_ID_NO_EXISTENT = "documentKey_bad";
	
	private static Document documentInput;
	private static DocumentChanges documentChanges;
	
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
    	
		List<String> allowedStatusTransitions1 = new ArrayList<>();
		allowedStatusTransitions1.add("ATTACHED");
		
		CurrentStatus currentStatus1 = new CurrentStatus();
		currentStatus1.setStorage("PN_LEGAL_FACTS");
		currentStatus1.setAllowedStatusTransitions(allowedStatusTransitions1);
		
		Map<String, CurrentStatus> statuses1 = new HashMap<>();
		statuses1.put("SAVED",currentStatus1);
	
		DocumentType docTypes = new DocumentType();
    	docTypes.setTipoDocumento(DOCTYPE_ID_LEGAL_FACTS);
    	docTypes.setChecksum(ChecksumEnum.SHA256); 
    	docTypes.setInitialStatus("SAVED");
    	docTypes.setStatuses(statuses1);
    	docTypes.setInformationClassification(InformationClassificationEnum.HC);
    	docTypes.setDigitalSignature(true);
    	docTypes.setTimeStamped(TimeStampedEnum.STANDARD);
		log.info("execute createDocument() : docType : {}", docTypes);
		
    	documentInput = new Document();
    	documentInput.setDocumentKey(PARTITION_ID_DEFAULT);
    	documentInput.setDocumentState(DocumentStateEnum.FREEZED);
    	documentInput.setRetentionUntil("2032-04-12T12:32:04.000Z");
    	documentInput.setCheckSum(CheckSumEnum.MD5);
		documentInput.setContentType("xxxxx");
		documentInput.setDocumentType(docTypes);
		documentInput.setContentLenght(new BigDecimal(100));
		log.info("execute createDocument() : documentInput : {}", documentInput);
		
		documentChanges = new DocumentChanges();
		documentChanges.setDocumentState(DocumentChanges.DocumentStateEnum.ATTACHED);
		documentChanges.setContentLenght(new BigDecimal(50));
	}
    	
    @Test
    // codice test: DCSS.101.1
    void postItem() {
    	
		EntityExchangeResult<DocumentResponse> resultPreInsert = webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(documentInput.getDocumentKey()))
		        .accept(APPLICATION_JSON)
		        .exchange()
		        .expectStatus().isOk()
		        .expectBody(DocumentResponse.class).returnResult();
    	
		log.info("\n Test 1 (postItem) resultPreInsert {} \n", resultPreInsert);
		
		if (resultPreInsert == null || resultPreInsert.getResponseBody() == null || resultPreInsert.getResponseBody().getDocument() == null) 
		{
	 		webTestClient.post()
		        .uri(BASE_PATH)
		        .accept(APPLICATION_JSON)
		        .contentType(APPLICATION_JSON)
		        .body(BodyInserters.fromValue(documentInput))
		        .exchange()
		        .expectStatus().isOk();
		}
		
		log.info("\n Test 1 (postItem) passed \n");
    }
    
    @Test
    // codice test: DCSS.101.2
    void postItemPartitionKeyDuplicated() {
    	
		EntityExchangeResult<DocumentResponse> resultPreInsert = webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(documentInput.getDocumentKey()))
		        .accept(APPLICATION_JSON)
		        .exchange()
		        .expectStatus().isOk()
		        .expectBody(DocumentResponse.class).returnResult();
    	
		log.info("\n Test 2 (postItemPartitionKeyDuplicated) resultPreInsert {} \n", resultPreInsert);
		
		if (resultPreInsert != null && resultPreInsert.getResponseBody() != null && resultPreInsert.getResponseBody().getDocument() != null)  {
	  
			webTestClient.post()
		        .uri(BASE_PATH)
		        .accept(APPLICATION_JSON)
		        .contentType(APPLICATION_JSON)
		        .body(BodyInserters.fromValue(documentInput))
		        .exchange()
		        .expectStatus().isEqualTo(HttpStatus.CONFLICT);
		}
			
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
    void getItemIncorrectParameters() {
    	
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
    	
		EntityExchangeResult<DocumentResponse> documentInDb = webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT))
		        .accept(APPLICATION_JSON)
		        .exchange()
		        .expectStatus().isOk()
		        .expectBody(DocumentResponse.class).returnResult();
		log.info("\n Test 6 (patchItem) : document before {}", documentInDb.getResponseBody().getDocument());
    	
    	log.info("\n Test 6 (patchItem) : documentChanges {}", documentChanges);
  
		webTestClient.patch()
	        .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT))
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(documentChanges))
	        .exchange()
	        .expectStatus().isOk();
		
		EntityExchangeResult<DocumentResponse> documentUpdated = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT))
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isOk()
	        .expectBody(DocumentResponse.class).returnResult();
		
		log.info("\n Test 6 (patchItem) : documentUpdated : {} \n", documentUpdated.getResponseBody().getDocument());
		
		Assertions.assertEquals(documentChanges.getContentLenght(), documentUpdated.getResponseBody().getDocument().getContentLenght());
	
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
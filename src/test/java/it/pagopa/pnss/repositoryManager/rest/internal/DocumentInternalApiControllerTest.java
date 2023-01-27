package it.pagopa.pnss.repositoryManager.rest.internal;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DocumentInternalApiControllerTest {
	
	@Autowired
	private WebTestClient webTestClient;

	private static final String BASE_URL = "/safe-storage/internal/v1/documents/";

	private static final String PARTITION_ID = "documentKey1";
	private static final String NO_EXISTENT_PARTITION_ID = "cdocumentKey_bad";
	
	private Document getDocument() {
		Document document = new Document();
		document.setDocumentKey(PARTITION_ID);
		document.setDocumentState(DocumentStateEnum.FREEZED);
		document.setRetentionPeriod("10000");
		document.setCheckSum(CheckSumEnum.MD5);
		document.contentLenght("10000");
		document.setContentType("xxxxx");
		document.setDocumentType(DocumentTypeEnum.NOTIFICATION_ATTACHMENTS);
		return document;
	}
    	
    @Test
    @Order(1)
    // codice test: DCSS.101.1
    public void postItem() {
  
    	Document documentInput = getDocument();

		webTestClient.post()
	        .uri(BASE_URL)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(documentInput))
	        .exchange()
	        .expectStatus().isOk();
		
		System.out.println("\n Test 1 (postItem) passed \n");
    	
    }
    
    @Test
    @Order(2)
    // codice test: DCSS.101.2
    public void postItemPartitionKeyDuplicated() {
  
    	Document documentInput = getDocument();

		webTestClient.post()
	        .uri(BASE_URL)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(documentInput))
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
			
		System.out.println("\n Test 2 (postItemPartitionKeyDuplicated) insert 2");
		
		System.out.println("\n Test 2 (postItemPartitionKeyDuplicated) passed \n");
    	
    }
    
    @Test
    @Order(3)
    // codice test: DCSS.100.1
    public void getItem() {
    	
		webTestClient.get()
			.uri(BASE_URL+"/"+PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isOk()
	        .expectBody(Document.class);
	    
	    System.out.println("\n Test 3 (getItem) passed \n");
  
    }
    
    @Test
    @Order(4)
    // codice test: DCSS.100.2
    public void getItemNoExistentPartitionKey() {
    	
		webTestClient.get()
			.uri(BASE_URL+"/"+NO_EXISTENT_PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .exchange()
			.expectStatus().isOk()
			.expectBody().isEmpty();
	    
	    System.out.println("\n Test 4 (getItemNoExistentPartitionKey) passed \n");
  
    }
    
    @Test
    @Order(5)
    // codice test: DCSS.100.3
    public void getItemIncorrectParameters() {
    	
		webTestClient.get()
			.uri(BASE_URL/*+"/"+NO_EXISTENT_PARTITION_ID*/)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
	    
	    System.out.println("\n Test 5 (getItemIncorrectParameters) passed \n");
  
    }
	
    @Test
    @Order(6)
    // codice test: DCSS.102.1
    public void patchItem() {
  
    	Document documentInput = getDocument();
    	documentInput.setDocumentState(DocumentStateEnum.AVAILABLE);
    	
		webTestClient.patch()
	        .uri(BASE_URL + "/" + PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(documentInput))
	        .exchange()
	        .expectStatus().isOk();
		
		EntityExchangeResult<Document> documentUpdated = webTestClient.get()
			.uri(BASE_URL + "/" + PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isOk()
	        .expectBody(Document.class).returnResult();
		
		Assertions.assertEquals(documentInput.getDocumentState(), documentUpdated.getResponseBody().getDocumentState());
	
		System.out.println("\n Test 6 (patchItem) passed \n");
    	
    }
    
    @Test
    @Order(7)
    // codice test: DCSS.102.2
    public void patchItemNoExistentKey() {
  
    	Document documentInput = getDocument();
    	documentInput.setDocumentKey(NO_EXISTENT_PARTITION_ID);
    	
		webTestClient.patch()
	        .uri(BASE_URL + "/" + NO_EXISTENT_PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(documentInput))
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	
		System.out.println("\n Test 7 (patchItemNoExistentKey) passed \n");
    	
    }
    
    @Test
    @Order(8)
    // codice test: DCSS.102.3
    public void patchItemIncorretcParameters() {
  
    	Document documentInput = getDocument();
    	
		webTestClient.patch()
	        .uri(BASE_URL /*+ "/" + PARTITION_ID*/)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(documentInput))
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
	
		System.out.println("\n Test 8 (patchItemIncorretcParameters) passed \n");
    	
    }
    
    @Test
    @Order(9)
    // codice test: DCSS.103.1
    public void deleteItem() {
    	
		webTestClient.delete()
			.uri(BASE_URL+"/"+PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isOk()
	        .expectBody().isEmpty();
	    
	    System.out.println("\n Test 9 (deleteItem) passed \n");
  
    }
    
    @Test
    @Order(10)
    // codice test: DCSS.103.2
    public void deleteItemNoExistentKey() {
    	
		webTestClient.delete()
			.uri(BASE_URL+"/"+NO_EXISTENT_PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	    
	    System.out.println("\n Test 10 (deleteItemNoExistentKey) passed \n");
  
    }
    
    @Test
    @Order(11)
    // codice test: DCSS.103.3
    public void deleteItemIncorrectParameter() {
    	
		webTestClient.delete()
			.uri(BASE_URL/*+"/"+NO_EXISTENT_PARTITION_ID*/)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
	    
	    System.out.println("\n Test 10 (deleteItemIncorrectParameter) passed \n");
  
    }
    
}
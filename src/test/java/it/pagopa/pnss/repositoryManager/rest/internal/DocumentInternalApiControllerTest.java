package it.pagopa.pnss.repositoryManager.rest.internal;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import it.pagopa.pnss.repositoryManager.dto.DocumentInput;
import it.pagopa.pnss.repositoryManager.dto.DocumentOutput;
import it.pagopa.pnss.repositoryManager.dto.DocumentStateEnum;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DocumentInternalApiControllerTest {
	
	@Autowired
	private WebTestClient webTestClient;

	private static final String BASE_URL = "http://localhost:8080//document";

	private static final String PARTITION_ID = "checksum1";
	private static final String NO_EXISTENT_PARTITION_ID = "checksum_bad";
    	
    @Test
    @Order(1)
    // codice test: DCSS.101.1
    public void postItem() {
  
    	DocumentInput documentInput = new DocumentInput();
    	documentInput.setCheckSum(PARTITION_ID);
//    	documentInput.setContentLenght("prova content lenght 2");
//    	documentInput.setContentType("prova content type 2");
//    	documentInput.setDocumentKey("chiavedocumento2");
//    	documentInput.setDocumentState("stato prova");
    	documentInput.setDocumentState(DocumentStateEnum.AVAILABLE);
//    	documentInput.setDocumentType("tipo prova");
    	documentInput.setRetentionPeriod("retention prova");

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
  
    	DocumentInput documentInput = new DocumentInput();
    	documentInput.setCheckSum(PARTITION_ID);
    	documentInput.setDocumentState(DocumentStateEnum.AVAILABLE);
    	documentInput.setRetentionPeriod("retention prova");

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
	        .expectBody(DocumentOutput.class);
	    
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
	        .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	    
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
    public void putItem() {
  
    	DocumentInput documentInput = new DocumentInput();
    	documentInput.setCheckSum(PARTITION_ID);
//    	documentInput.setContentLenght("prova content lenght 3");
//    	documentInput.setContentType("prova content type 3");
//    	documentInput.setDocumentKey("chiavedocumento2");
//    	documentInput.setDocumentState("stato prova 3");
    	documentInput.setDocumentState(DocumentStateEnum.AVAILABLE);
//    	documentInput.setDocumentType("tipo prova 3");
    	documentInput.setRetentionPeriod("retention prova 3");
    	
		webTestClient.put()
	        .uri(BASE_URL)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(documentInput))
	        .exchange()
	        .expectStatus().isOk();
	
		System.out.println("\n Test 6 (putItem) passed \n");
    	
    }
    
    @Test
    @Order(7)
    // codice test: DCSS.102.2
    public void putItemNoExistentKey() {
  
    	DocumentInput documentInput = new DocumentInput();
    	documentInput.setCheckSum(NO_EXISTENT_PARTITION_ID);
    	documentInput.setDocumentState(DocumentStateEnum.AVAILABLE);
    	documentInput.setRetentionPeriod("retention prova 3");
    	
		webTestClient.put()
	        .uri(BASE_URL)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(documentInput))
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	
		System.out.println("\n Test 7 (putItemNoExistentKey) passed \n");
    	
    }
    
    @Test
    @Order(8)
    // codice test: DCSS.102.3
    public void putItemIncorretcParameters() {
  
    	DocumentInput documentInput = new DocumentInput();
//    	documentInput.setCheckSum(NO_EXISTENT_PARTITION_ID);
    	documentInput.setDocumentState(DocumentStateEnum.AVAILABLE);
    	documentInput.setRetentionPeriod("retention prova 3");
    	
		webTestClient.put()
	        .uri(BASE_URL)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(documentInput))
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	
		System.out.println("\n Test 8 (putItemIncorretcParameters) passed \n");
    	
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
	        .expectBody(DocumentOutput.class);
	    
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
package it.pagopa.pnss.repositoryManager.rest.internal;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.ArrayList;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import it.pagopa.pnss.repositoryManager.dto.UserConfigurationDestinationDTO;
import it.pagopa.pnss.repositoryManager.dto.UserConfigurationInput;
import it.pagopa.pnss.repositoryManager.dto.UserConfigurationOutput;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserConfigurationInternalApiControllerTest {

	@Autowired
	private WebTestClient webTestClient;
	
	private static final String PARTITION_ID = "provachiave1";
	private static final String SORT_ID = "sortedKey1";
	private static final String NO_EXISTENT_PARTITION_ID = "prova_chiave_bad";
	
	private static final String BASE_URL = "http://localhost:8090/user-configuration";
	
	@Test
	@Order(1)
	// codice test: ANSS.101.1
	public void postItem() {
		
		UserConfigurationInput userInput = new UserConfigurationInput();
		userInput.setName(PARTITION_ID);
		userInput.setApiKey(SORT_ID);
		userInput.setCanCreate(new ArrayList<String>());
		userInput.getCanCreate().add("create1");
		userInput.setCanRead(new ArrayList<String>());
		userInput.getCanRead().add("read1");
		userInput.setDestination(new UserConfigurationDestinationDTO());
		userInput.getDestination().setSqsUrl("url1");
		userInput.setSignatureInfo("info1");
		
		webTestClient.post()
	        .uri(BASE_URL)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(userInput))
	        .exchange()
	        .expectStatus()
	        .isOk();
		
		System.out.println("\n Test 1 (postItem) insert 1");
		
		System.out.println("\n Test 1 (postItem) passed \n");
	}
	
    @Test
    @Order(2)
    // codice test: ANSS.101.1
    public void postItemPartitionKeyDuplicated() {
    	
		UserConfigurationInput userInput = new UserConfigurationInput();
		userInput.setName(PARTITION_ID);
		userInput.setApiKey(SORT_ID);
		userInput.setCanCreate(new ArrayList<String>());
		userInput.getCanCreate().add("create1");
		userInput.setCanRead(new ArrayList<String>());
		userInput.getCanRead().add("read1");
		userInput.setDestination(new UserConfigurationDestinationDTO());
		userInput.getDestination().setSqsUrl("url1");
		userInput.setSignatureInfo("info1");
    	
		webTestClient.post()
		        .uri(BASE_URL)
		        .accept(APPLICATION_JSON)
		        .contentType(APPLICATION_JSON)
		        .body(BodyInserters.fromValue(userInput))
		        .exchange()
		        .expectStatus()
		        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		
		System.out.println("\n Test 2 (postItemPartitionKeyDuplicated) insert 2");
	
		System.out.println("\n Test 2 (postItemPartitionKeyDuplicated) passed \n");
    }
	
	@Test
	@Order(3)
	// codice test: ANSS.100.1
	public void getItem() {

		webTestClient.get()
			.uri(BASE_URL+"/"+PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus()
	        .isOk()
	        .expectBody(UserConfigurationOutput.class);
        
        System.out.println("\n Test 3 (getItem) passed \n");
	}
	
	@Test
	@Order(4)
	// codice test: ANSS.100.2
	public void getItemIncorrectParameters() {

		webTestClient.get()
			.uri(BASE_URL/*+"/"+PARTITION_ID*/)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus()
	        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        
        System.out.println("\n Test 4 (getItemIncorrectParameters) passed \n");
	}
	
	@Test
	@Order(5)
	// codice test: ANSS.100.3
	public void getItemNoExistentKey() {

		webTestClient.get()
			.uri(BASE_URL+"/"+NO_EXISTENT_PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        
        System.out.println("\n Test 5 (getItemNoExistentKey) passed \n");
	}
	
    @Test
    @Order(6)
    // codice test: ANSS.102.1
    public void putItem() {
    	
		UserConfigurationInput userInput = new UserConfigurationInput();
		userInput.setName(PARTITION_ID);
		userInput.setApiKey(SORT_ID);
		userInput.setCanCreate(new ArrayList<String>());
    	userInput.getCanCreate().add("create2");
    	userInput.setCanRead(new ArrayList<String>());
    	userInput.getCanRead().add("read2");
    	userInput.setDestination(new UserConfigurationDestinationDTO());
    	userInput.getDestination().setSqsUrl("url2");
    	userInput.setSignatureInfo("info2");
    	
		webTestClient.put()
	        .uri(BASE_URL)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(userInput))
	        .exchange()
	        .expectStatus()
	        .isOk();
	
		System.out.println("\n Test 6 (putItem) passed \n");
    }
    
    @Test
    @Order(7)
    // codice test: ANSS.102.2
    public void putItemNoExistentKey() {
    	
		UserConfigurationInput userInput = new UserConfigurationInput();
		userInput.setName(NO_EXISTENT_PARTITION_ID);
		userInput.setApiKey(SORT_ID);
		userInput.setCanCreate(new ArrayList<String>());
    	userInput.getCanCreate().add("create2");
    	userInput.setCanRead(new ArrayList<String>());
    	userInput.getCanRead().add("read2");
    	userInput.setDestination(new UserConfigurationDestinationDTO());
    	userInput.getDestination().setSqsUrl("url2");
    	userInput.setSignatureInfo("info2");
    	
		webTestClient.put()
	        .uri(BASE_URL)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(userInput))
	        .exchange()
	        .expectStatus()
	        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	
		System.out.println("\n Test 7 (putItemNoExistentKey) passed \n");
    }
    
    @Test
    @Order(8)
    // codice test: ANSS.102.3
    public void putItemIncorrectParameters() {
    	
		UserConfigurationInput userInput = new UserConfigurationInput();
		userInput.setName(NO_EXISTENT_PARTITION_ID);
//		userInput.setApiKey(SORT_ID);
		userInput.setCanCreate(new ArrayList<String>());
    	userInput.getCanCreate().add("create2");
    	userInput.setCanRead(new ArrayList<String>());
    	userInput.getCanRead().add("read2");
    	userInput.setDestination(new UserConfigurationDestinationDTO());
    	userInput.getDestination().setSqsUrl("url2");
    	userInput.setSignatureInfo("info2");
    	
		webTestClient.put()
	        .uri(BASE_URL)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(userInput))
	        .exchange()
	        .expectStatus()
	        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	
		System.out.println("\n Test 8 (putItemIncorrectParameters) passed \n");
    }
    
    @Test
    @Order(9)
    // codice test: ANSS.103.1
    public void deleteItem() {
    	
		webTestClient.delete()
			.uri(BASE_URL+"/"+PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus()
	        .isOk()
	        .expectBody(UserConfigurationOutput.class)
	        ;
	    
	    System.out.println("\n Test 9 (deleteItem) passed \n");
    }
    
    @Test
    @Order(10)
    // codice test: ANSS.103.2
    public void deleteItemNoExistentKey() {
    	
		webTestClient.delete()
			.uri(BASE_URL+"/"+NO_EXISTENT_PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus()
	        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	    
	    System.out.println("\n Test 10 (deleteItemNoExistentKey) passed \n");
    }
    
    @Test
    @Order(11)
    // codice test: ANSS.103.2
    public void deleteItemIncorrectParametes() {
    	
		webTestClient.delete()
			.uri(BASE_URL/*+"/"+NO_EXISTENT_PARTITION_ID*/)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus()
	        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
	    
	    System.out.println("\n Test 11 (deleteItemIncorrectParametes) passed \n");
    }

}

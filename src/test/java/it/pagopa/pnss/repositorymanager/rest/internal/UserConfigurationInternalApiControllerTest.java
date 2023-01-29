package it.pagopa.pnss.repositorymanager.rest.internal;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.List;

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

import it.pagopa.pn.template.internal.rest.v1.dto.UserConfiguration;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfigurationDestination;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class UserConfigurationInternalApiControllerTest {

	@Autowired
	private WebTestClient webTestClient;
	
	private static final String PARTITION_ID = "provachiave1";
	private static final String NO_EXISTENT_PARTITION_ID = "prova_chiave_bad";
	
	private static final String BASE_URL = "http://localhost:8080/safestorage/internal/v1/userConfigurations";
	
	private UserConfiguration getUserConfiguration() {
		List<String> canCreate = new ArrayList<>();
		canCreate.add("A");
		List<String> canRead  = new ArrayList<>();
		canRead.add("DD");
		UserConfigurationDestination destination = new UserConfigurationDestination(); 
		destination.setSqsUrl("URL");
		
		UserConfiguration userConfiguration = new UserConfiguration();
		userConfiguration.setName(PARTITION_ID);
		userConfiguration.setCanCreate(canCreate);
		userConfiguration.setCanRead(canRead);
		userConfiguration.setSignatureInfo("mmm");
		userConfiguration.setDestination(destination);
		userConfiguration.setApiKey("apiKey");
		return userConfiguration;
	}
	
	@Test
	@Order(1)
	// codice test: ANSS.101.1
	public void postItem() {
		
		UserConfiguration userInput = getUserConfiguration();
		
		webTestClient.post()
	        .uri(BASE_URL)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(userInput))
	        .exchange()
	        .expectStatus().isOk();
		
		log.info("\n Test 1 (postItem) insert 1");
		
		log.info("\n Test 1 (postItem) passed \n");
	}
	
    @Test
    @Order(2)
    // codice test: ANSS.101.1
    public void postItemPartitionKeyDuplicated() {
    	
    	UserConfiguration userInput = getUserConfiguration();
    	
		webTestClient.post()
		        .uri(BASE_URL)
		        .accept(APPLICATION_JSON)
		        .contentType(APPLICATION_JSON)
		        .body(BodyInserters.fromValue(userInput))
		        .exchange()
		        .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		
		log.info("\n Test 2 (postItemPartitionKeyDuplicated) insert 2");
	
		log.info("\n Test 2 (postItemPartitionKeyDuplicated) passed \n");
    }
	
	@Test
	@Order(3)
	// codice test: ANSS.100.1
	public void getItem() {

		webTestClient.get()
			.uri(BASE_URL+"/"+PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isOk()
	        .expectBody(UserConfiguration.class);
        
        log.info("\n Test 3 (getItem) passed \n");
	}
	
	@Test
	@Order(4)
	// codice test: ANSS.100.2
	public void getItemIncorrectParameter() {

		webTestClient.get()
			.uri(BASE_URL/*+"/"+PARTITION_ID*/)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        
        log.info("\n Test 4 (getItemIncorrectParameters) passed \n");
	}
	
	@Test
	@Order(5)
	// codice test: ANSS.100.3
	public void getItemNoExistentKey() {

		webTestClient.get()
			.uri(BASE_URL+"/"+NO_EXISTENT_PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isOk()
	        .expectBody().isEmpty();
        
        log.info("\n Test 5 (getItemNoExistentKey) passed \n");
	}
	
    @Test
    @Order(6)
    // codice test: ANSS.102.1
    public void patchItem() {
    	
		List<String> canCreate = new ArrayList<>();
		canCreate.add("B");
    	
		UserConfiguration userInput = getUserConfiguration();
    	userInput.setCanCreate(canCreate);
    	
		webTestClient.patch()
	        .uri(BASE_URL+"/"+PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(userInput))
	        .exchange()
	        .expectStatus().isOk();
		
		EntityExchangeResult<UserConfiguration> userConfigurationUpdated = webTestClient.get()
				.uri(BASE_URL + "/" + PARTITION_ID)
		        .accept(APPLICATION_JSON)
		        .exchange()
		        .expectStatus().isOk()
		        .expectBody(UserConfiguration.class).returnResult();
			
		Assertions.assertEquals(userInput.getCanCreate(), userConfigurationUpdated.getResponseBody().getCanCreate());
	
		log.info("\n Test 6 (patchItem) passed \n");
    }
    
    @Test
    @Order(7)
    // codice test: ANSS.102.2
    public void patchItemNoExistentKey() {
    	
    	UserConfiguration userInput = getUserConfiguration();
    	userInput.setName(NO_EXISTENT_PARTITION_ID);
    	
		webTestClient.patch()
	        .uri(BASE_URL+"/"+NO_EXISTENT_PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(userInput))
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	
		log.info("\n Test 7 (NO_EXISTENT_PARTITION_ID) passed \n");
    }
    
    @Test
    @Order(8)
    // codice test: ANSS.102.3
    public void patchItemIncorrectParameter() {
    	
    	UserConfiguration userInput = getUserConfiguration();
    	
		webTestClient.patch()
	        .uri(BASE_URL /*+ "/" + PARTITION_ID*/)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(userInput))
	        .exchange()
	        .expectStatus()
	        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
	
		log.info("\n Test 8 (patchItemIncorrectParameters) passed \n");
    }
    
    @Test
    @Order(9)
    // codice test: ANSS.103.1
    public void deleteItem() {
    	
		webTestClient.delete()
			.uri(BASE_URL+"/"+PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isOk()
	        .expectBody().isEmpty();	        ;
	    
	    log.info("\n Test 9 (deleteItem) passed \n");
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
	    
	    log.info("\n Test 10 (deleteItemNoExistentKey) passed \n");
    }
    
    @Test
    @Order(11)
    // codice test: ANSS.103.2
    public void deleteItemIncorrectParameter() {
    	
		webTestClient.delete()
			.uri(BASE_URL/*+"/"+NO_EXISTENT_PARTITION_ID*/)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus()
	        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
	    
	    log.info("\n Test 11 (deleteItemIncorrectParametes) passed \n");
    }

}

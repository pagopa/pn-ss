package it.pagopa.pnss.repositoryManager.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.Collections;
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

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.ChecksumEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.InformationClassificationEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.TimeStampedEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.TipoDocumentoEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfiguration;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfigurationDestination;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class ConfigurationApiControllerTest {

	@Autowired
	private WebTestClient webTestClient;
	
	private static final String BASE_URL_DOC_TYPE = "http://localhost:8080/safestorage/internal/v1/doctypes";
	private static final String BASE_URL_USER_CONFIGURATION = "http://localhost:8080/safestorage/internal/v1/userConfigurations";
	private static final String BASE_URL_CONFIGURATIONS_DOC_TYPE = "http://localhost:8080/safe-storage/v1/configurations/documents-types";
	private static final String BASE_URL_CONFIGURATIONS_USER_CONF = "http://localhost:8080/safe-storage/v1/configurations/clients/";
	
	private static final String PARTITION_ID_NAME_KEY_1 = "key1";
	final String PARTITION_ID_NAME_KEY_2 = "key2";


	private DocumentType getDocumentType(TipoDocumentoEnum name) {
		DocumentType docTypesInput = new DocumentType();
		docTypesInput.setTipoDocumento(name);
		docTypesInput.setChecksum(ChecksumEnum.MD5);
		docTypesInput.setLifeCycleTag("lifeCicle1");
		docTypesInput.setInformationClassification(InformationClassificationEnum.C);
		docTypesInput.setDigitalSignature(true);
		docTypesInput.setTimeStamped(TimeStampedEnum.STANDARD);
		return docTypesInput;
	}
	
	private UserConfiguration getUserConfiguration(String name) {
		List<String> canCreate = new ArrayList<>();
		canCreate.add("A");
		List<String> canRead  = new ArrayList<>();
		canRead.add("DD");
		UserConfigurationDestination destination = new UserConfigurationDestination(); 
		destination.setSqsUrl("URL");
		
		UserConfiguration userConfiguration = new UserConfiguration();
		userConfiguration.setName(name);
		userConfiguration.setCanCreate(canCreate);
		userConfiguration.setCanRead(canRead);
		userConfiguration.setSignatureInfo("mmm");
		userConfiguration.setDestination(destination);
		userConfiguration.setApiKey("apiKey");
		return userConfiguration;
	}

	@Test
	@Order(1)
	public void getDocumentsConfigs() {
		
		final TipoDocumentoEnum namePrimo = TipoDocumentoEnum.AAR;
		DocumentType docTypePrimoInput = getDocumentType(namePrimo);

		webTestClient.post()
			.uri(BASE_URL_DOC_TYPE)
			.accept(APPLICATION_JSON)
			.contentType(APPLICATION_JSON)
			.body(BodyInserters.fromValue(docTypePrimoInput))
			.exchange()
			.expectStatus().isOk();
		
		log.info("Test 1. getDocumentsConfigs() : docType (Primo Input) inserito : {}", docTypePrimoInput);
		
		TipoDocumentoEnum nameSecondo = TipoDocumentoEnum.LEGAL_FACTS;
		DocumentType docTypeSecondoInput = getDocumentType(nameSecondo);

		webTestClient.post()
			.uri(BASE_URL_DOC_TYPE)
			.accept(APPLICATION_JSON)
			.contentType(APPLICATION_JSON)
			.body(BodyInserters.fromValue(docTypeSecondoInput))
			.exchange()
			.expectStatus().isOk();
		
		log.info("Test 1. getDocumentsConfigs() : docType (Secondo Input) inserito : {}", docTypeSecondoInput);
		
		EntityExchangeResult<DocumentTypesConfigurations> docTypeInserted = webTestClient.get()
				.uri(BASE_URL_CONFIGURATIONS_DOC_TYPE)
		        .accept(APPLICATION_JSON)
		        .exchange()
		        .expectStatus().isOk()
		        .expectBody(DocumentTypesConfigurations.class).returnResult();
		
		DocumentTypesConfigurations result = docTypeInserted.getResponseBody();
		
		log.info("Test 1. getDocumentsConfigs() : get list docTypes : {}", docTypeInserted.getResponseBody());
		
		Assertions.assertNotNull(result);
		Assertions.assertNotNull(result.getDocumentsTypes());
		Assertions.assertEquals(2,result.getDocumentsTypes().size());
		
//		List<String> expected = new ArrayList<>();
//		expected.add(namePrimo.getValue());
//		expected.add(nameSecondo.getValue());
//		Collections.sort(expected);
//		
//		List<String> returned = new ArrayList<>();
//		returned.add(result.getDocumentsTypes().get(0).getName());
//		returned.add(result.getDocumentsTypes().get(1).getName());
//		Collections.sort(returned);
//
//		Assertions.assertEquals(expected, returned);
//		
//		log.info("Test 1. getDocumentsConfigs() : test passed");
//		
//		webTestClient.delete()
//			.uri(BASE_URL_DOC_TYPE+"/"+ namePrimo.getValue())
//	        .accept(APPLICATION_JSON)
//	        .exchange()
//	        .expectStatus().isOk();
//		
//		webTestClient.delete()
//			.uri(BASE_URL_DOC_TYPE+"/"+ nameSecondo.getValue())
//	        .accept(APPLICATION_JSON)
//	        .exchange()
//	        .expectStatus().isOk();

	}
	
	@Test
	@Order(2)
	public void getCurrentClientConfig() {
		
		
		UserConfiguration userConfigurationInput = getUserConfiguration(PARTITION_ID_NAME_KEY_1);
		
		webTestClient.post()
	        .uri(BASE_URL_USER_CONFIGURATION)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(userConfigurationInput))
	        .exchange()
	        .expectStatus().isOk();
		
		log.info("\n Test 2. getCurrentClientConfig() : userConfiguration inserito");
		
		EntityExchangeResult<it.pagopa.pn.template.rest.v1.dto.UserConfiguration> userConfigurationInserted = webTestClient.get()
			.uri(BASE_URL_CONFIGURATIONS_USER_CONF+"/"+PARTITION_ID_NAME_KEY_1)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isOk()
	        .expectBody(it.pagopa.pn.template.rest.v1.dto.UserConfiguration.class).returnResult();
	    
	    log.info("\n Test 2 (getCurrentClientConfig) get userConfiguration \n");
	    
	    Assertions.assertNotNull(userConfigurationInserted.getResponseBody());
	    Assertions.assertEquals(userConfigurationInput.getName(), userConfigurationInserted.getResponseBody().getName());
	    
	    log.info("\n Test 2 (getCurrentClientConfig) test passed \n");
		
	}
	
	@Test
	@Order(3)
	public void getCurrentClientConfigNoExistentKey() {
		
//		UserConfiguration userConfigurationInput = getUserConfiguration(PARTITION_ID_NAME_KEY_1);
//		
//		webTestClient.post()
//	        .uri(BASE_URL_USER_CONFIGURATION)
//	        .accept(APPLICATION_JSON)
//	        .contentType(APPLICATION_JSON)
//	        .body(BodyInserters.fromValue(userConfigurationInput))
//	        .exchange()
//	        .expectStatus().isOk();
//		
//		log.info("\n Test 3. getCurrentClientConfigNoExistentKey() : userConfiguration inserito, name {} \n", userConfigurationInput.getName());
		
		webTestClient.get()
			.uri(BASE_URL_CONFIGURATIONS_USER_CONF+"/"+PARTITION_ID_NAME_KEY_2)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	    
	    log.info("\n Test 3 (getCurrentClientConfigNoExistentKey) test passed \n");
		
	}
	
	@Test
	@Order(4)
	public void getCurrentClientIncorrectParameter() {
				
		webTestClient.get()
			.uri(BASE_URL_CONFIGURATIONS_USER_CONF)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
	    
	    log.info("\n Test 3 (getCurrentClientIncorrectParameter) test passed \n");
		
	}
	

}

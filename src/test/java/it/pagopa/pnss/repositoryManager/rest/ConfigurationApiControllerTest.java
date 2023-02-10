package it.pagopa.pnss.repositoryManager.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.pagopa.pn.template.internal.rest.v1.dto.CurrentStatus;
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
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfiguration;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfigurationDestination;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositoryManager.entity.UserConfigurationEntity;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Slf4j
public class ConfigurationApiControllerTest {

	@Autowired
	private WebTestClient webTestClient;
	
	private static final String BASE_URL_DOC_TYPE = "/safestorage/internal/v1/doctypes";
	private static final String BASE_URL_DOC_TYPE_WITH_PARAM = String.format("%s/{typeId}", BASE_URL_DOC_TYPE);

	private static final String BASE_URL_USER_CONFIGURATION = "/safestorage/internal/v1/userConfigurations";
	
	private static final String BASE_URL_CONFIGURATIONS_DOC_TYPE = "/safe-storage/v1/configurations/documents-types";
	
	private static final String BASE_PATH_CONFIGURATIONS_USER_CONF = "/safe-storage/v1/configurations/clients/";
	private static final String BASE_PATH_CONFIGURATIONS_USER_CONF_WITH_PARAM = String.format("%s/{clientId}", BASE_PATH_CONFIGURATIONS_USER_CONF);
	
	private static final String PARTITION_ID_DEFAULT_USER_CONF = "key1";
	private static final String PARTITION_ID_NO_EXISTENT_USER_CONF = "key2";
	
	private static UserConfiguration userConfigurationInput;
	
	private static DynamoDbTable<UserConfigurationEntity> dynamoDbTable;
	
    private static void insertUserConfigurationEntity(String name) {
    	log.info("execute insertUserConfigurationEntity()");
        var userConfigurationEntity = new UserConfigurationEntity();
        userConfigurationEntity.setName(name);
        dynamoDbTable.putItem(builder -> builder.item(userConfigurationEntity));
    }
	
    @BeforeAll
    public static void insertDefaultUserConfiguration(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient,
    		@Autowired RepositoryManagerDynamoTableName gestoreRepositoryDynamoDbTableName) 
    {
    	log.info("execute insertDefaultUserConfiguration()");
        dynamoDbTable = dynamoDbEnhancedClient.table(
//        		DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME, 
        		gestoreRepositoryDynamoDbTableName.anagraficaClientName(),
        		TableSchema.fromBean(UserConfigurationEntity.class));
        insertUserConfigurationEntity(PARTITION_ID_DEFAULT_USER_CONF);
    }
	
	@BeforeEach
	public void createUserConfiguration() {
		List<String> canCreate = new ArrayList<>();
		canCreate.add("A");
		List<String> canRead  = new ArrayList<>();
		canRead.add("DD");
		UserConfigurationDestination destination = new UserConfigurationDestination(); 
		destination.setSqsUrl("URL");
		
		userConfigurationInput = new UserConfiguration();
		userConfigurationInput.setName(PARTITION_ID_DEFAULT_USER_CONF);
		userConfigurationInput.setCanCreate(canCreate);
		userConfigurationInput.setCanRead(canRead);
		userConfigurationInput.setSignatureInfo("mmm");
		userConfigurationInput.setDestination(destination);
		userConfigurationInput.setApiKey("apiKey");
	}
	
	private DocumentType getDocumentType(TipoDocumentoEnum name) {
		List<Map<String, CurrentStatus>> statuses = new ArrayList<>();
		Map<String, CurrentStatus> status = new HashMap<>();
		CurrentStatus currentStatus = new CurrentStatus();
		List<String> allowedStatusTransitions = new ArrayList<>();
		currentStatus.setStorage("PN_TEMPORARY_DOCUMENT");
		allowedStatusTransitions.add("ATTACHED");
		currentStatus.setAllowedStatusTransitions(allowedStatusTransitions);
		status.put("PRELOADED",currentStatus);
		statuses.add(status);



		DocumentType docTypesInput = new DocumentType();
		docTypesInput.setTipoDocumento(name);
		docTypesInput.setChecksum(ChecksumEnum.MD5);
		docTypesInput.setStatuses(statuses);
		docTypesInput.setInformationClassification(InformationClassificationEnum.C);
		docTypesInput.setDigitalSignature(true);
		docTypesInput.setTimeStamped(TimeStampedEnum.STANDARD);
		return docTypesInput;
	}

	@Test
	void getDocumentsConfigs() {
		log.info("Test 1. getDocumentsConfigs() : START");
		
		//TODO ripristinare, dopo aver aggiunto lifecycleRule per bucket relativo a PnSsBucketName
		
//		final TipoDocumentoEnum namePrimo = TipoDocumentoEnum.AAR;
//		DocumentType docTypePrimoInput = getDocumentType(namePrimo);
//		
//		EntityExchangeResult<DocumentType> resultPrimo =
//			webTestClient.get()
//				.uri(uriBuilder -> uriBuilder.path(BASE_URL_DOC_TYPE_WITH_PARAM).build(namePrimo.getValue()))
//				.accept(APPLICATION_JSON)
//				.exchange()
//				.expectBody(DocumentType.class).returnResult();
//		boolean inseritoPrimo = false;
//		if (resultPrimo != null && resultPrimo.getResponseBody() != null) 
//		{
//			webTestClient.post()
//				.uri(BASE_URL_DOC_TYPE)
//				.accept(APPLICATION_JSON)
//				.contentType(APPLICATION_JSON)
//				.body(BodyInserters.fromValue(docTypePrimoInput))
//				.exchange()
//				.expectStatus().isOk();
//			
//			inseritoPrimo = true;
//			
//			log.info("Test 1. getDocumentsConfigs() : docType (Primo Input) inserito : {}", docTypePrimoInput);
//		}
//		else {
//			log.info("Test 1. getDocumentsConfigs() : docType (Primo Input) presente : key {}", namePrimo.getValue());
//		}
//		
//		TipoDocumentoEnum nameSecondo = TipoDocumentoEnum.EXTERNAL_LEGAL_FACTS;
//		DocumentType docTypeSecondoInput = getDocumentType(nameSecondo);
//
//		EntityExchangeResult<DocumentType> resultSecondo =
//				webTestClient.get()
//					.uri(uriBuilder -> uriBuilder.path(BASE_URL_DOC_TYPE_WITH_PARAM).build(nameSecondo.getValue()))
//					.accept(APPLICATION_JSON)
//					.exchange()
//					.expectBody(DocumentType.class).returnResult();
//		boolean inseritoSecondo = false;
//		if (resultSecondo != null && resultSecondo.getResponseBody() != null) 
//		{
//			webTestClient.post()
//				.uri(BASE_URL_DOC_TYPE)
//				.accept(APPLICATION_JSON)
//				.contentType(APPLICATION_JSON)
//				.body(BodyInserters.fromValue(docTypeSecondoInput))
//				.exchange()
//				.expectStatus().isOk();
//			
//			inseritoSecondo = true;
//			
//			log.info("Test 1. getDocumentsConfigs() : docType (Secondo Input) inserito : {}", docTypeSecondoInput);
//		}
//		else {
//			log.info("Test 1. getDocumentsConfigs() : docType (Secondo Input) presente : key {}", nameSecondo.getValue());
//		}
//		
//		EntityExchangeResult<DocumentTypesConfigurations> docTypeInserted = webTestClient.get()
//				.uri(BASE_URL_CONFIGURATIONS_DOC_TYPE)
//		        .accept(APPLICATION_JSON)
//		        .exchange()
//		        .expectStatus().isOk()
//		        .expectBody(DocumentTypesConfigurations.class).returnResult();
//		
//		DocumentTypesConfigurations result = docTypeInserted.getResponseBody();
//		
//		log.info("Test 1. getDocumentsConfigs() : get list docTypes : {}", docTypeInserted.getResponseBody());
//		
//		Assertions.assertNotNull(result);
//		Assertions.assertNotNull(result.getDocumentsTypes());
////		Assertions.assertEquals(2,result.getDocumentsTypes().size());
//		
//		log.info("Test 1. getDocumentsConfigs() : test passed");
//		
//		if (inseritoPrimo) {
//			webTestClient.delete()
//				.uri(BASE_URL_DOC_TYPE+"/"+ namePrimo.getValue())
//		        .accept(APPLICATION_JSON)
//		        .exchange()
//		        .expectStatus().isOk();
//		}
//		
//		if (inseritoSecondo) {
//			webTestClient.delete()
//				.uri(BASE_URL_DOC_TYPE+"/"+ nameSecondo.getValue())
//		        .accept(APPLICATION_JSON)
//		        .exchange()
//		        .expectStatus().isOk();
//		}

	}
	
	@Test
	void getCurrentClientConfig() {
		
		EntityExchangeResult<it.pagopa.pn.template.rest.v1.dto.UserConfiguration> userConfigurationInserted = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(BASE_PATH_CONFIGURATIONS_USER_CONF_WITH_PARAM).build(PARTITION_ID_DEFAULT_USER_CONF))
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
	void getCurrentClientConfigNoExistentKey() {		
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(BASE_PATH_CONFIGURATIONS_USER_CONF_WITH_PARAM).build(PARTITION_ID_NO_EXISTENT_USER_CONF))
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
	    
	    log.info("\n Test 3 (getCurrentClientConfigNoExistentKey) test passed \n");
		
	}
	
	@Test
	void getCurrentClientIncorrectParameter() {
				
		webTestClient.get()
			.uri(BASE_PATH_CONFIGURATIONS_USER_CONF)
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
	    
	    log.info("\n Test 3 (getCurrentClientIncorrectParameter) test passed \n");
		
	}
	

}

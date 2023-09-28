package it.pagopa.pnss.repositorymanager.rest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.pagopa.pn.template.internal.rest.v1.dto.*;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositorymanager.exception.BucketException;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import it.pagopa.pnss.transformation.service.impl.S3ServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.UserConfigurationEntity;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.s3.model.*;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Slf4j
public class ConfigurationApiControllerTest {

	@Autowired
	private WebTestClient webTestClient;
	
	@Value("${header.x-api-key:#{null}}")
	private String xApiKey;
	@Value("${header.x-pagopa-safestorage-cx-id:#{null}}")
	private String xPagopaSafestorageCxId;
	
	private final String userConfigurationName =  "userClient2";
	private final String userConfigurationApiKey = "sortedKey2";
	private final String xApiKeyValue = userConfigurationApiKey;
	private final String xPagopaSafestorageCxIdValue = userConfigurationName;
	private static Mono<UserConfigurationResponse> userConfigurationResponse;
    @MockBean
    UserConfigurationClientCall userConfigurationClientCall;
	@MockBean
	S3ServiceImpl s3Service;
	@MockBean
	DocTypesService docTypesService;

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
		List<String> canModifyStatus = new ArrayList<>();
		canModifyStatus.add("ModifyStatus");
		UserConfigurationDestination destination = new UserConfigurationDestination(); 
		destination.setSqsUrl("URL");
		
		userConfigurationInput = new UserConfiguration();
		userConfigurationInput.setName(PARTITION_ID_DEFAULT_USER_CONF);
		userConfigurationInput.setCanCreate(canCreate);
		userConfigurationInput.setCanRead(canRead);
		userConfigurationInput.setCanModifyStatus(canModifyStatus);
		userConfigurationInput.setSignatureInfo("mmm");
		userConfigurationInput.setDestination(destination);
		userConfigurationInput.setApiKey("apiKey");
		
        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName(userConfigurationName);
        userConfiguration.setApiKey(userConfigurationApiKey);
        
        UserConfigurationResponse userConfig = new UserConfigurationResponse();
        userConfig.setUserConfiguration(userConfiguration);
        
        userConfigurationResponse = Mono.just(userConfig)  ;
	}
	
//	private DocumentType getDocumentType(String name) {
//		
//		List<String> allowedStatusTransitions = new ArrayList<>();
//		allowedStatusTransitions.add("ATTACHED");
//		
//		CurrentStatus currentStatus = new CurrentStatus();
//		currentStatus.setStorage("PN_TEMPORARY_DOCUMENT");
//		currentStatus.setAllowedStatusTransitions(allowedStatusTransitions);
//		
//		Map<String, CurrentStatus> statuses = new HashMap<>();
//		statuses.put("PRELOADED",currentStatus);
//
//		DocumentType docTypesInput = new DocumentType();
//		docTypesInput.setTipoDocumento(name);
//		docTypesInput.setChecksum(ChecksumEnum.MD5);
//		docTypesInput.setStatuses(statuses);
//		docTypesInput.setInformationClassification(InformationClassificationEnum.C);
//		docTypesInput.setDigitalSignature(true);
//		docTypesInput.setTimeStamped(TimeStampedEnum.STANDARD);
//		return docTypesInput;
//	}

	@Test
	void getDocumentsConfigs() {
		log.info("Test 1. getDocumentsConfigs() : START");

		List<DocumentType> documentTypeList = new ArrayList<>();
		createListDocType(documentTypeList);

		LifecycleRule lifecycleRule = createLifeCycleRule();

		GetBucketLifecycleConfigurationResponse getBucketResponse = GetBucketLifecycleConfigurationResponse.builder().rules(List.of(lifecycleRule)).build();

		when(s3Service.getBucketLifecycleConfiguration(anyString())).thenReturn(Mono.just(getBucketResponse));
		when(docTypesService.getAllDocumentType()).thenReturn(Mono.just(documentTypeList));

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
	}

	@Test
	void getDocumentsConfigsDocumentTypeNotPresentException() {

		LifecycleRule lifecycleRule = createLifeCycleRule();

		GetBucketLifecycleConfigurationResponse getBucketResponse = GetBucketLifecycleConfigurationResponse.builder().rules(List.of(lifecycleRule)).build();
		when(s3Service.getBucketLifecycleConfiguration(anyString())).thenReturn(Mono.just(getBucketResponse));
		when(docTypesService.getAllDocumentType()).thenReturn(Mono.error(new DocumentTypeNotPresentException("key")));

		webTestClient.get()
				.uri(BASE_URL_CONFIGURATIONS_DOC_TYPE)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void getDocumentsConfigsBucketException() {

		LifecycleRule lifecycleRule = createLifeCycleRule();

		GetBucketLifecycleConfigurationResponse getBucketResponse = GetBucketLifecycleConfigurationResponse.builder().rules(List.of(lifecycleRule)).build();
		when(s3Service.getBucketLifecycleConfiguration(anyString())).thenReturn(Mono.just(getBucketResponse));
		when(docTypesService.getAllDocumentType()).thenReturn(Mono.error(new BucketException()));

		webTestClient.get()
				.uri(BASE_URL_CONFIGURATIONS_DOC_TYPE)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void getDocumentsConfigsIdClientNotFoundException() {

		LifecycleRule lifecycleRule = createLifeCycleRule();

		GetBucketLifecycleConfigurationResponse getBucketResponse = GetBucketLifecycleConfigurationResponse.builder().rules(List.of(lifecycleRule)).build();
		when(s3Service.getBucketLifecycleConfiguration(anyString())).thenReturn(Mono.just(getBucketResponse));
		when(docTypesService.getAllDocumentType()).thenReturn(Mono.error(new IdClientNotFoundException("idClient123")));

		webTestClient.get()
				.uri(BASE_URL_CONFIGURATIONS_DOC_TYPE)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void getDocumentsConfigsException() {

		LifecycleRule lifecycleRule = createLifeCycleRule();

		GetBucketLifecycleConfigurationResponse getBucketResponse = GetBucketLifecycleConfigurationResponse.builder().rules(List.of(lifecycleRule)).build();
		when(s3Service.getBucketLifecycleConfiguration(anyString())).thenReturn(Mono.just(getBucketResponse));
		when(docTypesService.getAllDocumentType()).thenReturn(Mono.error(new Exception("Exception Generica")));

		webTestClient.get()
				.uri(BASE_URL_CONFIGURATIONS_DOC_TYPE)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	void getCurrentClientConfig() {
		
		log.info("\n Test 2 (getCurrentClientConfig) START \n");

        Mockito.doReturn(userConfigurationResponse).when(userConfigurationClientCall).getUser(Mockito.any());
		
		EntityExchangeResult<it.pagopa.pn.template.rest.v1.dto.UserConfiguration> userConfigurationInserted = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(BASE_PATH_CONFIGURATIONS_USER_CONF_WITH_PARAM).build(PARTITION_ID_DEFAULT_USER_CONF))
	        .accept(APPLICATION_JSON)
	        .header(xApiKey,xApiKeyValue)
	        .header(xPagopaSafestorageCxId,xPagopaSafestorageCxIdValue)
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
		
		log.info("\n Test 3 (getCurrentClientConfigNoExistentKey) START \n");
		
        Mockito.doReturn(userConfigurationResponse).when(userConfigurationClientCall).getUser(Mockito.any());
		
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(BASE_PATH_CONFIGURATIONS_USER_CONF_WITH_PARAM).build(PARTITION_ID_NO_EXISTENT_USER_CONF))
	        .accept(APPLICATION_JSON)
	        .header(xApiKey,xApiKeyValue)
	        .header(xPagopaSafestorageCxId,xPagopaSafestorageCxIdValue)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
	    
	    log.info("\n Test 3 (getCurrentClientConfigNoExistentKey) test passed \n");
	}
	
	@Test
	void getCurrentClientIncorrectParameter() {
		
		log.info("\n Test 4 (getCurrentClientIncorrectParameter) START \n");
		
		Mockito.doReturn(userConfigurationResponse).when(userConfigurationClientCall).getUser(Mockito.any());
				
		webTestClient.get()
			.uri(BASE_PATH_CONFIGURATIONS_USER_CONF)
			.accept(APPLICATION_JSON)
	        .header(xApiKey,xApiKeyValue)
	        .header(xPagopaSafestorageCxId,xPagopaSafestorageCxIdValue)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
	    
	    log.info("\n Test 4 (getCurrentClientIncorrectParameter) test passed \n");
	}

	void createListDocType(List<DocumentType> listDocTypes){
		DocumentType documentType1 = new DocumentType();
		DocumentType documentType2 = new DocumentType();

		documentType1.setTipoDocumento("PN_NOTIFICATION_ATTACHMENTS");
		documentType2.setTipoDocumento("PN_LEGAL_FACTS");

		documentType1.setInitialStatus("PRELOADED");
		documentType1.setInitialStatus("SAVED");

		CurrentStatus documentTypeConfigurationStatuses = new CurrentStatus();
		documentTypeConfigurationStatuses.setStorage("AVAILABLE");

		List<String> allowedStatusTransitions = new ArrayList<>();
		allowedStatusTransitions.add("ATTACHED");
		documentTypeConfigurationStatuses.setAllowedStatusTransitions(allowedStatusTransitions);

		documentType1.setStatuses(Map.of("AVAILABLE", documentTypeConfigurationStatuses));
		documentType2.setStatuses(Map.of("AVAILABLE", documentTypeConfigurationStatuses));

		documentType1.setInformationClassification(DocumentType.InformationClassificationEnum.HC);
		documentType2.setInformationClassification(DocumentType.InformationClassificationEnum.HC);

		documentType1.setTransformations(List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
		documentType2.setTransformations(List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));

		documentType1.setTimeStamped(DocumentType.TimeStampedEnum.STANDARD);
		documentType2.setTimeStamped(DocumentType.TimeStampedEnum.STANDARD);

		documentType1.setChecksum(DocumentType.ChecksumEnum.SHA256);
		documentType2.setChecksum(DocumentType.ChecksumEnum.SHA256);

		listDocTypes.add(documentType1);
		listDocTypes.add(documentType2);
	}

	LifecycleRule createLifeCycleRule(){

		List<Transition> transitions = new ArrayList<>();
		transitions.add(Transition.builder().days(1).build());
		transitions.add(Transition.builder().days(2).build());

		return LifecycleRule.builder()
				.id("01")
				.filter(LifecycleRuleFilter.builder()
						.and(LifecycleRuleAndOperator.builder()
								.prefix("prefix")
								.tags(Tag.builder()
										.key("storageType")
										.value("value")
										.build())
								.build())
						.build())
				.expiration(LifecycleExpiration.builder().days(500).build())
				.transitions(transitions)
				.build();
	}
}

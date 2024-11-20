package it.pagopa.pnss.repositorymanager.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.List;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfiguration;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfigurationDestination;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfigurationResponse;
import lombok.CustomLog;
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
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@CustomLog
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
	
	private static final String BASE_PATH_CONFIGURATIONS_USER_CONF = "/safe-storage/v1/configurations/clients/";
	private static final String BASE_PATH_CONFIGURATIONS_USER_CONF_WITH_PARAM = String.format("%s/{clientId}", BASE_PATH_CONFIGURATIONS_USER_CONF);
	private static final String PARTITION_ID_DEFAULT_USER_CONF = "key1";
	private static UserConfiguration userConfigurationInput;
	private static DynamoDbTable<UserConfigurationEntity> dynamoDbTable;
	
    private static void insertUserConfigurationEntity() {
    	log.info("execute insertUserConfigurationEntity()");
        var userConfigurationEntity = new UserConfigurationEntity();
        userConfigurationEntity.setName(ConfigurationApiControllerTest.PARTITION_ID_DEFAULT_USER_CONF);
        dynamoDbTable.putItem(builder -> builder.item(userConfigurationEntity));
    }
	
    @BeforeAll
    public static void insertDefaultUserConfiguration(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient,
    		@Autowired RepositoryManagerDynamoTableName gestoreRepositoryDynamoDbTableName) 
    {
    	log.info("execute insertDefaultUserConfiguration()");
        dynamoDbTable = dynamoDbEnhancedClient.table(
        		gestoreRepositoryDynamoDbTableName.anagraficaClientName(),
        		TableSchema.fromBean(UserConfigurationEntity.class));
        insertUserConfigurationEntity();
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
	
	@Test
	void getCurrentClientConfig() {
		
		log.info("\n Test 2 (getCurrentClientConfig) START \n");

        Mockito.doReturn(userConfigurationResponse).when(userConfigurationClientCall).getUser(Mockito.any());
		
		EntityExchangeResult<UserConfiguration> userConfigurationInserted = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(BASE_PATH_CONFIGURATIONS_USER_CONF_WITH_PARAM).build(PARTITION_ID_DEFAULT_USER_CONF))
	        .accept(APPLICATION_JSON)
	        .header(xApiKey,xApiKeyValue)
	        .header(xPagopaSafestorageCxId,xPagopaSafestorageCxIdValue)
	        .exchange()
	        .expectStatus().isOk()
	        .expectBody(UserConfiguration.class).returnResult();
	    
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
			.uri(uriBuilder -> uriBuilder.path(BASE_PATH_CONFIGURATIONS_USER_CONF_WITH_PARAM).build("NonExistingClient"))
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
}







package it.pagopa.pnss.repositorymanager.rest.internal;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfiguration;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfigurationChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfigurationDestination;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfigurationResponse;
import lombok.CustomLog;
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
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.UserConfigurationEntity;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@CustomLog
public class UserConfigurationInternalApiControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	private static final String BASE_PATH = "/safestorage/internal/v1/userConfigurations";
	private static final String BASE_PATH_WITH_PARAM = String.format("%s/{name}", BASE_PATH);
	private static final String PARTITION_ID_ENTITY = "key";
	private static final String PARTITION_ID_ENTITY_W_TAGS = "key2";
	private static final String PARTITION_ID_NO_EXISTENT = "name_bad";

	private static UserConfiguration userConfigurationInput;
	private static UserConfigurationChanges userConfigurationChanges;

	private static DynamoDbTable<UserConfigurationEntity> dynamoDbTable;

	private static void insertUserConfigurationEntity(String name) {
		log.info("execute insertUserConfigurationEntity()");
		var userConfigurationEntity = new UserConfigurationEntity();
		switch (name){
			case PARTITION_ID_ENTITY:
				break;
			case PARTITION_ID_ENTITY_W_TAGS:
				userConfigurationEntity.setCanWriteTags(true);
				userConfigurationEntity.setCanReadTags(true);
				break;
			default:
				throw new IllegalArgumentException("Name not defined");
		}
		userConfigurationEntity.setName(name);

		dynamoDbTable.putItem(builder -> builder.item(userConfigurationEntity));
	}


	@BeforeAll
	public static void insertDefaultUserConfigurations(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient,
													  @Autowired RepositoryManagerDynamoTableName gestoreRepositoryDynamoDbTableName) {
		log.info("execute insertDefaultDocType()");
		dynamoDbTable = dynamoDbEnhancedClient.table(
				gestoreRepositoryDynamoDbTableName.anagraficaClientName(),
				TableSchema.fromBean(UserConfigurationEntity.class));
				insertUserConfigurationEntity(PARTITION_ID_ENTITY);
		dynamoDbTable = dynamoDbEnhancedClient.table(
				gestoreRepositoryDynamoDbTableName.anagraficaClientName(),
				TableSchema.fromBean(UserConfigurationEntity.class));
		insertUserConfigurationEntity(PARTITION_ID_ENTITY_W_TAGS);
	}



	@BeforeEach
	public void createUserConfiguration() {
		List<String> canCreate = new ArrayList<>();
		canCreate.add("A");
		List<String> canRead = new ArrayList<>();
		canRead.add("DD");
		List<String> canModifyStatus = new ArrayList<>();
		canModifyStatus.add("ModifyStatus");
		UserConfigurationDestination destination = new UserConfigurationDestination();
		destination.setSqsUrl("URL");

		userConfigurationInput = new UserConfiguration();
		userConfigurationInput.setName(PARTITION_ID_ENTITY);
		userConfigurationInput.setCanCreate(canCreate);
		userConfigurationInput.setCanRead(canRead);
		userConfigurationInput.setCanModifyStatus(canModifyStatus);
		userConfigurationInput.setCanWriteTags(true);
		userConfigurationInput.setCanReadTags(true);
		userConfigurationInput.setSignatureInfo("mmm");
		userConfigurationInput.setDestination(destination);
		userConfigurationInput.setApiKey("apiKey");

		List<String> canCreate1 = new ArrayList<>();
		canCreate1.add("A");
		List<String> canRead1 = new ArrayList<>();
		canRead1.add("DD");
		List<String> canModifyStatus1 = new ArrayList<>();
		canModifyStatus1.add("ModifyStatus");

		userConfigurationChanges = new UserConfigurationChanges();
		userConfigurationChanges.setCanCreate(canCreate1);
		userConfigurationChanges.setCanRead(canRead1);
		userConfigurationChanges.setCanWriteTags(true);
		userConfigurationChanges.setCanReadTags(true);
		userConfigurationChanges.setCanModifyStatus(canModifyStatus1);
	}

	@Test
		// codice test: ANSS.101.1
	void postItem() {

		EntityExchangeResult<UserConfigurationResponse> resultPreInsert = webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(userConfigurationInput.getName()))
				.accept(APPLICATION_JSON).exchange().expectBody(UserConfigurationResponse.class).returnResult();

		log.info("\n Test 1 (postItem) resultPreInsert {} \n", resultPreInsert);

		if (resultPreInsert.getResponseBody() == null || resultPreInsert.getResponseBody().getUserConfiguration() == null) {
			webTestClient.post().uri(BASE_PATH).accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
					.body(BodyInserters.fromValue(userConfigurationInput)).exchange().expectStatus().isOk();
		}

		log.info("\n Test 1 (postItem) passed \n");
	}

	@Test
		// codice test: ANSS.101.1
	void postItemPartitionKeyDuplicated() {

		EntityExchangeResult<UserConfigurationResponse> resultPreInsert = webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(userConfigurationInput.getName()))
				.accept(APPLICATION_JSON).exchange().expectBody(UserConfigurationResponse.class).returnResult();

		log.info("\n Test 2 (postItemPartitionKeyDuplicated) resultPreInsert {} \n", resultPreInsert);

		if (resultPreInsert.getResponseBody() != null && resultPreInsert.getResponseBody().getUserConfiguration() != null) {
			webTestClient.post().uri(BASE_PATH).accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
					.body(BodyInserters.fromValue(userConfigurationInput)).exchange().expectStatus()
					.isEqualTo(HttpStatus.CONFLICT);

		}

		log.info("\n Test 2 (postItemPartitionKeyDuplicated) passed \n");
	}

	@Test
		// codice test: ANSS.101.1
	void postItemIncorrectParameter() {

		userConfigurationInput.setName(null);

		webTestClient.post().uri(BASE_PATH).accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
				.body(BodyInserters.fromValue(userConfigurationInput)).exchange().expectStatus()
				.isEqualTo(HttpStatus.BAD_REQUEST);

		log.info("\n Test 2 (postItemIncorrectParameter) passed \n");
	}

	@Test
		// codice test: ANSS.100.1
	void getItem() {

		webTestClient.get().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_ENTITY))
				.accept(APPLICATION_JSON).exchange().expectStatus().isOk().expectBody(UserConfigurationResponse.class);

		log.info("\n Test 3 (getItem) passed \n");
	}

	@Test
	void getItemWithTags() {
		EntityExchangeResult<UserConfigurationResponse> response = webTestClient.get().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_ENTITY_W_TAGS))
				.accept(APPLICATION_JSON).exchange().expectStatus().isOk().expectBody(UserConfigurationResponse.class).returnResult();

		Assertions.assertEquals(userConfigurationInput.getCanReadTags(), Objects.requireNonNull(response.getResponseBody()).getUserConfiguration().getCanReadTags());
		Assertions.assertEquals(userConfigurationInput.getCanWriteTags(), response.getResponseBody().getUserConfiguration().getCanWriteTags());
	}

	@Test
		// codice test: ANSS.100.2
	void getItemIncorrectParameter() {

		webTestClient.get().uri(BASE_PATH).accept(APPLICATION_JSON).exchange().expectStatus()
				.isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);

		log.info("\n Test 4 (getItemIncorrectParameters) passed \n");
	}

	@Test
		// codice test: ANSS.100.3
	void getItemNoExistentKey() {

		webTestClient.get().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_NO_EXISTENT))
				.accept(APPLICATION_JSON).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);

		log.info("\n Test 5 (getItemNoExistentKey) passed \n");
	}

	@Test
		// codice test: ANSS.102.1
	void patchItem() {

		EntityExchangeResult<UserConfigurationResponse> documentInDb = webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_ENTITY))
				.accept(APPLICATION_JSON).exchange().expectStatus().isOk().expectBody(UserConfigurationResponse.class)
				.returnResult();
		log.info("\n Test 6 (patchItem) : userConfiguration before patch {}",
				Objects.requireNonNull(documentInDb.getResponseBody()).getUserConfiguration());

		log.info("\n Test 6 (patchItem) : userConfigurationChanges {}", userConfigurationChanges);

		webTestClient.patch().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_ENTITY))
				.accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
				.body(BodyInserters.fromValue(userConfigurationChanges)).exchange().expectStatus().isOk();

		EntityExchangeResult<UserConfigurationResponse> userConfigurationUpdated = webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_ENTITY))
				.accept(APPLICATION_JSON).exchange().expectStatus().isOk().expectBody(UserConfigurationResponse.class)
				.returnResult();

		log.info("\n Test 6 (patchItem) userConfigurationUpdated2 : {} \n",
				Objects.requireNonNull(userConfigurationUpdated.getResponseBody()).getUserConfiguration());

		Assertions.assertEquals(userConfigurationChanges.getCanCreate(),
				userConfigurationUpdated.getResponseBody().getUserConfiguration().getCanCreate());

		log.info("\n Test 6 (patchItem) passed \n");
	}

	@Test
	void patchItemWithTags() {

		EntityExchangeResult<UserConfigurationResponse> documentInDb = webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_ENTITY))
				.accept(APPLICATION_JSON).exchange().expectStatus().isOk().expectBody(UserConfigurationResponse.class)
				.returnResult();
		log.info("\n Test 6 (patchItem) : userConfiguration before patch {}",
				Objects.requireNonNull(documentInDb.getResponseBody()).getUserConfiguration());

		log.info("\n Test 6 (patchItem) : userConfigurationChanges {}", userConfigurationChanges);

		webTestClient.patch().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_ENTITY))
				.accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
				.body(BodyInserters.fromValue(userConfigurationChanges)).exchange().expectStatus().isOk();

		EntityExchangeResult<UserConfigurationResponse> userConfigurationUpdated = webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_ENTITY))
				.accept(APPLICATION_JSON).exchange().expectStatus().isOk().expectBody(UserConfigurationResponse.class)
				.returnResult();

		log.info("\n Test 6 (patchItem) userConfigurationUpdated2 : {} \n",
				Objects.requireNonNull(userConfigurationUpdated.getResponseBody()).getUserConfiguration());

		Assertions.assertEquals(userConfigurationChanges.getCanCreate(),
				userConfigurationUpdated.getResponseBody().getUserConfiguration().getCanCreate());
		Assertions.assertEquals(userConfigurationChanges.getCanRead(),
				userConfigurationUpdated.getResponseBody().getUserConfiguration().getCanRead());
		Assertions.assertEquals(userConfigurationChanges.getCanModifyStatus(),
				userConfigurationUpdated.getResponseBody().getUserConfiguration().getCanModifyStatus());
		Assertions.assertEquals(userConfigurationChanges.getCanWriteTags(),
				userConfigurationUpdated.getResponseBody().getUserConfiguration().getCanWriteTags());
		Assertions.assertEquals(userConfigurationChanges.getCanReadTags(),
				userConfigurationUpdated.getResponseBody().getUserConfiguration().getCanReadTags());

		log.info("\n Test 6 (patchItem) passed \n");
	}

	@Test
		// codice test: ANSS.102.2
	void patchItemNoExistentKey() {

		userConfigurationInput.setName(PARTITION_ID_NO_EXISTENT);

		webTestClient.patch().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_NO_EXISTENT))
				.accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
				.body(BodyInserters.fromValue(userConfigurationInput)).exchange().expectStatus()
				.isEqualTo(HttpStatus.NOT_FOUND);

		log.info("\n Test 7 (patchItemNoExistentKey) passed \n");
	}

	@Test
		// codice test: ANSS.102.3
	void patchItemIncorrectParameter() {

		webTestClient.patch().uri(BASE_PATH).accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
				.body(BodyInserters.fromValue(userConfigurationInput)).exchange().expectStatus()
				.isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);

		log.info("\n Test 8 (patchItemIncorrectParameters) passed \n");
	}

	@Test
		// codice test: ANSS.103.1
	void deleteItem() {

		webTestClient.delete().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_ENTITY))
				.accept(APPLICATION_JSON).exchange().expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

		log.info("\n Test 9 (deleteItem) passed \n");
	}

	@Test
		// codice test: ANSS.103.2
	void deleteItemNoExistentKey() {

		webTestClient.delete().uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(PARTITION_ID_NO_EXISTENT))
				.accept(APPLICATION_JSON).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);

		log.info("\n Test 10 (deleteItemNoExistentKey) passed \n");
	}

	@Test
		// codice test: ANSS.103.2
	void deleteItemIncorrectParameter() {

		webTestClient.delete().uri(BASE_PATH).accept(APPLICATION_JSON).exchange().expectStatus()
				.isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);

		log.info("\n Test 11 (deleteItemIncorrectParametes) passed \n");
	}

}

package it.pagopa.pnss.indexing;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.CurrentStatusEntity;
import it.pagopa.pnss.repositorymanager.entity.DocTypeEntity;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pnss.common.constant.Constant.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient(timeout = "36000")
@CustomLog
public class
AdditionalFileTagsGetTest {

    @Value("${header.x-api-key:#{null}}")
    private String xApiKey;
    @Value("${header.x-pagopa-safestorage-cx-id:#{null}}")
    private String xPagopaSafestorageCxId;
    @Value("${test.aws.s3.endpoint:#{null}}")
    String testAwsS3Endpoint;

    @Autowired
    private WebTestClient webTestClient;
    private static final String GET_PATH_WITH_PARAM = "/safe-storage/v1/files/{fileKey}/tags";
    @MockBean
    private UserConfigurationClientCall userConfigurationClientCall;
    @MockBean
    private DocumentClientCall documentClientCall;

    private static DynamoDbTable<DocumentEntity> dynamoDbTable;
    private static final String DOCTYPE_ID_LEGAL_FACTS = "PN_NOTIFICATION_ATTACHMENTS";
    private static final String PARTITION_ID_ENTITY_TAGS = "documentKeyEntDocTags";
    private static final String PARTITION_ID_NO_EXISTENT = "documentKeyEntTagsNotExist";
    private static final String PARTITION_ID_DEFAULT_TAGS = PARTITION_ID_ENTITY_TAGS;
    private static final String X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE = "CLIENT_ID_123";
    private static final String X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE_NOT_EXIST = "pn-unknown";


    private static Map<String, List<String>> createTagsList() {
        Map<String, List<String>> tags = new HashMap<>();

        List<String> valuesForKey1 = new ArrayList<>();
        valuesForKey1.add("value_1");
        valuesForKey1.add("value_7");
        tags.put("pn-client~key_1", valuesForKey1);

        List<String> valuesForKey2 = new ArrayList<>();
        valuesForKey2.add("value_2");
        tags.put("key_2", valuesForKey2);

        List<String> valuesForKey3 = new ArrayList<>();
        valuesForKey3.add("value_3");
        valuesForKey3.add("value_9");
        valuesForKey3.add("value_8");
        tags.put("key_3", valuesForKey3);

        List<String> valuesForKey4 = new ArrayList<>();
        valuesForKey4.add("value_4");
        tags.put("key_4", valuesForKey4);
        return tags;
    }

    private static void insertDocumentEntityWithTags(String documentKey) {
        log.info("execute insertDocumentEntity()");

        List<String> allowedStatusTransitions1 = new ArrayList<>();
        allowedStatusTransitions1.add("AVAILABLE");

        CurrentStatusEntity currentStatus1 = new CurrentStatusEntity();
        currentStatus1.setStorage(DOCTYPE_ID_LEGAL_FACTS);
        currentStatus1.setAllowedStatusTransitions(allowedStatusTransitions1);
        currentStatus1.setTechnicalState("SAVED");

        Map<String, CurrentStatusEntity> statuses1 = new HashMap<>();
        statuses1.put("SAVED", currentStatus1);

        DocTypeEntity docTypeEntity = new DocTypeEntity();
        docTypeEntity.setTipoDocumento(DOCTYPE_ID_LEGAL_FACTS);
        docTypeEntity.setStatuses(statuses1);
        log.info("execute insertDocumentEntityTags() : docTypeEntity : {}", docTypeEntity);

        var documentEntity = new DocumentEntity();
        documentEntity.setDocumentKey(documentKey);
        documentEntity.setDocumentType(docTypeEntity);
        documentEntity.setContentLenght(new BigDecimal(50));
        documentEntity.setLastStatusChangeTimestamp(OffsetDateTime.now());
        documentEntity.setDocumentState(SAVED);
        documentEntity.setDocumentLogicalState(AVAILABLE);
        documentEntity.setTags(createTagsList());
        log.info("execute insertDocumentEntityTags() : documentEntity : {}", documentEntity);

        dynamoDbTable.putItem(builder -> builder.item(documentEntity));
    }

    @BeforeAll
    public static void insertDefaultDocument(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient,
                                             @Autowired RepositoryManagerDynamoTableName gestoreRepositoryDynamoDbTableName) {
        log.info("execute insertDefaultDocument()");
        dynamoDbTable = dynamoDbEnhancedClient.table(
                gestoreRepositoryDynamoDbTableName.documentiName(), TableSchema.fromBean(DocumentEntity.class));
        insertDocumentEntityWithTags(PARTITION_ID_ENTITY_TAGS);

    }


    @Test
    void getItemWithTags() {
        Document document1 = new Document();
        document1.setDocumentKey(PARTITION_ID_DEFAULT_TAGS);
        document1.setTags(createTagsList());

        when(userConfigurationClientCall.getUser(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE)).thenReturn(Mono.just(new UserConfigurationResponse()
                .userConfiguration(new UserConfiguration().name(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE).apiKey("apiKey_value").canReadTags(true))));
        when(documentClientCall.getDocument(PARTITION_ID_DEFAULT_TAGS))
                .thenReturn(Mono.just(new DocumentResponse().document(document1)));

        var response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(GET_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT_TAGS))
                .accept(APPLICATION_JSON)
                .header(xPagopaSafestorageCxId, X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE)
                .header(xApiKey, "apiKey_value")
                .exchange()
                .expectStatus().isOk()
                .expectBody(AdditionalFileTagsGetResponse.class).returnResult();
        String responseBody = response.getResponseBody() != null ? String.valueOf(response.getResponseBody()) : "Response body is null";
        log.info("\n Response Body: {}\n", responseBody);

        log.info("\n Test 3 (getItem) passed \n");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void getItemWithoutTags(Map<String, List<String>> tagsMap) {
        Document document1 = new Document();
        document1.setDocumentKey(PARTITION_ID_DEFAULT_TAGS);
        document1.setTags(tagsMap);

        when(userConfigurationClientCall.getUser(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE)).thenReturn(Mono.just(new UserConfigurationResponse()
                .userConfiguration(new UserConfiguration().name(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE).apiKey("apiKey_value").canReadTags(true))));
        when(documentClientCall.getDocument(PARTITION_ID_DEFAULT_TAGS))
                .thenReturn(Mono.just(new DocumentResponse().document(document1)));

        var response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(GET_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT_TAGS))
                .accept(APPLICATION_JSON)
                .header(xPagopaSafestorageCxId, X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE)
                .header(xApiKey, "apiKey_value")
                .exchange()
                .expectStatus().isOk()
                .expectBody(AdditionalFileTagsGetResponse.class).returnResult();
        String responseBody = response.getResponseBody() != null ? String.valueOf(response.getResponseBody()) : "Response body is null";
        log.info("\n Response Body: {}\n", responseBody);

        log.info("\n Test 3 (getItem) passed \n");
    }


    @Test
    void getItemNoExistentPartitionKey() {
        when(userConfigurationClientCall.getUser(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE)).thenReturn(Mono.just(new UserConfigurationResponse()
                .userConfiguration(new UserConfiguration().name(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE).apiKey("apiKey_value").canReadTags(true))));
        when(documentClientCall.getDocument(PARTITION_ID_NO_EXISTENT))
                .thenReturn(Mono.error(new DocumentKeyNotPresentException("Document key not found.")));

        webTestClient.get().uri(uriBuilder -> uriBuilder.path(GET_PATH_WITH_PARAM).build(PARTITION_ID_NO_EXISTENT))
                .accept(APPLICATION_JSON)
                .header(xPagopaSafestorageCxId, X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE)
                .header(xApiKey, "apiKey_value")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.NOT_FOUND)
                .expectBody();
        log.info("\n Test 4 (getItemNoExistentPartitionKey) passed \n");

    }

    @Test
    @SneakyThrows
    void getItemWithClientNotValid() {
        when(userConfigurationClientCall.getUser(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE_NOT_EXIST)).thenReturn(Mono.error(new IdClientNotFoundException("Client not authorized.")));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(GET_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT_TAGS))
                .accept(APPLICATION_JSON)
                .header("x-pagopa-safestorage-cx-id", X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE_NOT_EXIST)
                .header("x-api-key", "pn-test_api_key")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN)
                .expectBody();
        log.info("\n Test 4 (getItemWithClientNotValid) passed \n");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false})
    @SneakyThrows
    void getItemWithClientNotAuthorized(Boolean canReadTags) {
        when(userConfigurationClientCall.getUser(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE_NOT_EXIST)).thenReturn(Mono.just(new UserConfigurationResponse()
                .userConfiguration(new UserConfiguration().name(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE).apiKey("apiKey_value").canReadTags(canReadTags))));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(GET_PATH_WITH_PARAM).build(PARTITION_ID_DEFAULT_TAGS))
                .accept(APPLICATION_JSON)
                .header("x-pagopa-safestorage-cx-id", X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE_NOT_EXIST)
                .header("x-api-key", "pn-test_api_key")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN)
                .expectBody();
        log.info("\n Test 4 (getItemWithClientNotValid) passed \n");
    }


}

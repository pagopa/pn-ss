package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.ScadenzaDocumentiInput;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.ScadenzaDocumentiResponse;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.ScadenzaDocumentiEntity;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Instant;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@CustomLog
public class ScadenzaDocumentiInternalApiControllerTest {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private static DynamoDbTable<ScadenzaDocumentiEntity> dynamoTable;
    private static final String BASE_PATH = "/safestorage/internal/v1/scadenza-documenti";

    @BeforeAll
    public static void setup(@Autowired DynamoDbEnhancedClient enhancedClient,
                             @Autowired RepositoryManagerDynamoTableName gestoreRepositoryDynamoDbTableName) {
        log.info("execute insertScadenzaDocumenti()");
        dynamoTable = enhancedClient.table(
                gestoreRepositoryDynamoDbTableName.documentiName(), TableSchema.fromBean(ScadenzaDocumentiEntity.class));
        ScadenzaDocumentiEntity scadenzaDocumenti = new ScadenzaDocumentiEntity();
        scadenzaDocumenti.setRetentionUntil(Instant.EPOCH.plusSeconds(31536000).getEpochSecond());
        scadenzaDocumenti.setDocumentKey("documentKey");

        dynamoTable.putItem(builder -> builder.item(scadenzaDocumenti));
    }

    @Test
    void insertOrUpdateScadenzaDocumentiTestOk() {
        EntityExchangeResult<ScadenzaDocumentiResponse> result = webTestClient.post()
                .uri(BASE_PATH)
                .bodyValue(new ScadenzaDocumentiInput().documentKey("documentKey").retentionUntil(Instant.EPOCH.plusSeconds(31536000).getEpochSecond()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ScadenzaDocumentiResponse.class)
                .returnResult();
    }

    @Test
    void insertOrUpdateScadenzaDocumentiIfAlreadyExistsTestOk() {
        EntityExchangeResult<ScadenzaDocumentiResponse> result = webTestClient.post()
                .uri(BASE_PATH)
                .bodyValue(new ScadenzaDocumentiInput().documentKey("documentKey").retentionUntil(Instant.EPOCH.plusSeconds(31536000).getEpochSecond()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ScadenzaDocumentiResponse.class)
                .value(response -> log.info("response: {}", response))
                .returnResult();
    }

    @Test
    void insertOrUpdateWithDateBeforeTestKo() {
        webTestClient.post()
                .uri(BASE_PATH)
                .bodyValue(new ScadenzaDocumentiInput().documentKey("documentKey").retentionUntil(Instant.EPOCH.minusSeconds(31536000).getEpochSecond()))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ScadenzaDocumentiResponse.class)
                .value(response -> log.info("response: {}", response))
                .returnResult();

    }

    @Test
    void insertOrUpdateWithEmptyInputIsKo() {
        webTestClient.post()
                .uri(BASE_PATH)
                .bodyValue(new ScadenzaDocumentiInput())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ScadenzaDocumentiResponse.class)
                .value(response -> log.info("response: {}", response))
                .returnResult();

    }


}

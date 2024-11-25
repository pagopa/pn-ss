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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
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
    @Value("${gestore.repository.anagrafica.internal.scadenza.documenti.post}")
    private String basePath;
    private static final String DOCUMENT_KEY = "documentKey";
    private static final long SECONDS_TO_ADD = 31536000;

    @BeforeAll
    public static void setup(@Autowired DynamoDbEnhancedClient enhancedClient, @Autowired RepositoryManagerDynamoTableName gestoreRepositoryDynamoDbTableName) {
        log.info("execute insertScadenzaDocumenti()");
        DynamoDbTable<ScadenzaDocumentiEntity> dynamoTable = enhancedClient.table(gestoreRepositoryDynamoDbTableName.documentiName(), TableSchema.fromBean(ScadenzaDocumentiEntity.class));
        ScadenzaDocumentiEntity scadenzaDocumenti = new ScadenzaDocumentiEntity();
        scadenzaDocumenti.setRetentionUntil(Instant.EPOCH.plusSeconds(SECONDS_TO_ADD).getEpochSecond());
        scadenzaDocumenti.setDocumentKey(DOCUMENT_KEY);
        dynamoTable.putItem(builder -> builder.item(scadenzaDocumenti));
    }

    @Test
    void insertOrUpdateScadenzaDocumentiTestOk() {
        webTestClient.post()
                .uri(basePath)
                .bodyValue(new ScadenzaDocumentiInput().documentKey(DOCUMENT_KEY).retentionUntil(Instant.EPOCH.plusSeconds(SECONDS_TO_ADD).getEpochSecond()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ScadenzaDocumentiResponse.class)
                .returnResult();
    }

    @Test
    void insertOrUpdateScadenzaDocumentiIfAlreadyExistsTestOk() {
        webTestClient.post()
                .uri(basePath)
                .bodyValue(new ScadenzaDocumentiInput().documentKey(DOCUMENT_KEY).retentionUntil(Instant.EPOCH.plusSeconds(SECONDS_TO_ADD).getEpochSecond()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ScadenzaDocumentiResponse.class)
                .value(response -> log.info("Response: {}", response))
                .returnResult();
    }

    @Test
    void insertOrUpdateWithDateBeforeTestKo() {
        webTestClient.post()
                .uri(basePath)
                .bodyValue(new ScadenzaDocumentiInput().documentKey(DOCUMENT_KEY).retentionUntil(Instant.EPOCH.minusSeconds(SECONDS_TO_ADD).getEpochSecond()))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ScadenzaDocumentiResponse.class)
                .value(response -> log.info("Response: {}", response))
                .returnResult();

    }

    @Test
    void insertOrUpdateWithEmptyInputIsKo() {
        webTestClient.post()
                .uri(basePath)
                .bodyValue(new ScadenzaDocumentiInput())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ScadenzaDocumentiResponse.class)
                .value(response -> log.info("Response: {}", response))
                .returnResult();

    }


}

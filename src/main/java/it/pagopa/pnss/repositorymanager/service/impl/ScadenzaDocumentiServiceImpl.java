package it.pagopa.pnss.repositorymanager.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.ScadenzaDocumenti;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.ScadenzaDocumentiInput;
import it.pagopa.pnss.common.exception.IdemPotentElementException;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.ScadenzaDocumentiEntity;
import it.pagopa.pnss.repositorymanager.exception.InvalidRetentionException;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.ScadenzaDocumentiService;
import lombok.CustomLog;
import org.apache.tika.utils.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import java.time.Instant;


@Service
@CustomLog
public class ScadenzaDocumentiServiceImpl implements ScadenzaDocumentiService {
    private final ObjectMapper objectMapper;
    private final DynamoDbAsyncTableDecorator<ScadenzaDocumentiEntity> scadenzaDocumentiDynamoDbAsyncTable;


    public ScadenzaDocumentiServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                        RepositoryManagerDynamoTableName repositoryManagerDynamoTableName,
                                        ObjectMapper objectMapper) {
        this.scadenzaDocumentiDynamoDbAsyncTable = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedAsyncClient
                .table(repositoryManagerDynamoTableName.scadenzaDocumentiName(),
                        TableSchema.fromBean(ScadenzaDocumentiEntity.class)));
        this.objectMapper = objectMapper;


    }


    @Override
    public Mono<ScadenzaDocumenti> insertOrUpdateScadenzaDocumenti(ScadenzaDocumentiInput scadenzaDocumentiInput) {
        final String INSERT_SCADENZA_DOCUMENTI = "insertScadenzaDocumenti()";
        log.debug(LogUtils.INVOKING_METHOD, INSERT_SCADENZA_DOCUMENTI, scadenzaDocumentiInput);

        return validateInput(scadenzaDocumentiInput)
                .flatMap(entity -> Mono.fromCompletionStage(scadenzaDocumentiDynamoDbAsyncTable.putItem(builder -> builder.item(entity))).thenReturn(entity))
                .map(entity -> objectMapper.convertValue(entity, ScadenzaDocumenti.class))
                .onErrorResume(IdemPotentElementException.class, throwable -> {
                            ScadenzaDocumenti scadenzaDocumenti = new ScadenzaDocumenti();
                            scadenzaDocumenti.setRetentionUntil(scadenzaDocumentiInput.getRetentionUntil());
                            scadenzaDocumenti.setDocumentKey(scadenzaDocumentiInput.getDocumentKey());
                            return Mono.just(scadenzaDocumenti);
                        }
                )
                .onErrorResume(IllegalArgumentException.class, throwable -> {
                            log.error("insertScadenzaDocumenti() : IllegalArgumentException : {}", throwable.getMessage());
                            return Mono.error(new RepositoryManagerException(throwable.getMessage()));
                        }
                )
                .doOnSuccess(response -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, INSERT_SCADENZA_DOCUMENTI, response));
    }



    private Mono<ScadenzaDocumentiEntity> validateInput(ScadenzaDocumentiInput scadenzaDocumentiInput){
        return Mono.justOrEmpty(scadenzaDocumentiInput)
                .switchIfEmpty(Mono.error(new RepositoryManagerException("Input is null")))
                .cast(ScadenzaDocumentiInput.class)
                .flatMap(input -> {
                    if (StringUtils.isBlank(input.getDocumentKey()) || input.getRetentionUntil() == null) {
                        // controllo che il documentKey non sia vuoto
                        return Mono.error(new RepositoryManagerException("One of the attributes is null or empty"));
                    } else {
                        return Mono.just(input);
                    }
                })
                .flatMap(input -> Mono.fromCompletionStage(() -> scadenzaDocumentiDynamoDbAsyncTable.getItem(Key.builder().partitionValue(input.getDocumentKey()).build())))
                .handle((existingEntity, sink) -> {
                    if (scadenzaDocumentiInput.getRetentionUntil().equals(existingEntity.getRetentionUntil())) {
                        // se il documento è uguale a quello già presente non faccio nulla
                        sink.error(new IdemPotentElementException(scadenzaDocumentiInput.getDocumentKey()));
                    } else {
                        sink.next(existingEntity);
                    }
                })
                .cast(ScadenzaDocumentiEntity.class)
                .handle((scadenzaDocumentiFound, sink) -> {
                    //verifico che la retention until inserita non sia minore di quella già presente
                    Instant retentionUntilInput =Instant.ofEpochSecond(scadenzaDocumentiInput.getRetentionUntil());
                    Instant retentionUntilFounded = Instant.ofEpochSecond(scadenzaDocumentiFound.getRetentionUntil());
                    if (retentionUntilInput.isBefore(retentionUntilFounded)) {
                        log.debug("insertScadenzaDocumenti() : the inserted retentionUntil : {} is before the existing one : {}", retentionUntilInput, retentionUntilFounded);
                        sink.error(new InvalidRetentionException("Retention until is invalid. The inserted retentionUntil  is before the existing one."));
                    } else {
                        sink.next(scadenzaDocumentiInput);
                    }
                })
                .switchIfEmpty(Mono.just(scadenzaDocumentiInput))
                .cast(ScadenzaDocumentiInput.class)
                .map(scadenzaDocumentiInputConverted -> {
                    ScadenzaDocumentiEntity entity = new ScadenzaDocumentiEntity();
                    entity.setDocumentKey(scadenzaDocumentiInput.getDocumentKey());
                    entity.setRetentionUntil(scadenzaDocumentiInput.getRetentionUntil());
                    return entity;
                })
                .doOnSuccess(entity -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, "validateInput()", entity));
    }
}

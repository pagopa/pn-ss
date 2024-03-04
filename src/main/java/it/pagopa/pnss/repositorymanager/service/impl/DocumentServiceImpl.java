package it.pagopa.pnss.repositorymanager.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Document;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentInput;
import it.pagopa.pnss.common.constant.*;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.model.pojo.DocumentStatusChange;
import it.pagopa.pnss.common.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pnss.common.retention.RetentionService;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configurationproperties.AwsConfigurationProperties;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.CurrentStatusEntity;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.repositorymanager.exception.IllegalDocumentStateException;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import it.pagopa.pnss.repositorymanager.service.DocumentService;
import it.pagopa.pnss.transformation.service.S3Service;
import lombok.CustomLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.utils.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.s3.model.Tagging;

import java.lang.reflect.ParameterizedType;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.constant.Constant.STORAGE_TYPE;
import static it.pagopa.pnss.common.utils.DynamoDbUtils.DYNAMO_OPTIMISTIC_LOCKING_RETRY;


@Service
@CustomLog
public class DocumentServiceImpl implements DocumentService {

    private final ObjectMapper objectMapper;
    private final DynamoDbAsyncTableDecorator<DocumentEntity> documentEntityDynamoDbAsyncTable;
    private final DocTypesService docTypesService;
    private final RetentionService retentionService;
    private final AwsConfigurationProperties awsConfigurationProperties;
    private final BucketName bucketName;
    private final CallMacchinaStati callMacchinaStati;
    private final S3Service s3Service;
    @Autowired
    RepositoryManagerDynamoTableName managerDynamoTableName;

    public DocumentServiceImpl(ObjectMapper objectMapper, DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                               RepositoryManagerDynamoTableName repositoryManagerDynamoTableName, DocTypesService docTypesService,
                               RetentionService retentionService, AwsConfigurationProperties awsConfigurationProperties,
                               BucketName bucketName, CallMacchinaStati callMacchinaStati, S3Service s3Service) {
        this.docTypesService = docTypesService;
        this.callMacchinaStati = callMacchinaStati;
        this.s3Service = s3Service;
        this.documentEntityDynamoDbAsyncTable = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedAsyncClient.table(repositoryManagerDynamoTableName.documentiName(),
                                                                                  TableSchema.fromBean(DocumentEntity.class)));
        this.objectMapper = objectMapper;
        this.retentionService = retentionService;
        this.awsConfigurationProperties = awsConfigurationProperties;
        this.bucketName = bucketName;
    }

    private Mono<DocumentEntity> getErrorIdDocNotFoundException(String documentKey) {
        return Mono.error(new DocumentKeyNotPresentException(documentKey));
    }

    @Override
    public Mono<Document> getDocument(String documentKey) {
        final String GET_DOCUMENT = "DocumentService.getDocument()";
        log.debug(LogUtils.INVOKING_METHOD, GET_DOCUMENT, documentKey);
        return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(documentKey).build()))
                   .switchIfEmpty(getErrorIdDocNotFoundException(documentKey))
                   .doOnError(DocumentKeyNotPresentException.class, throwable -> log.debug(throwable.getMessage()))
                   .map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, Document.class));
    }

    @Override
    public Mono<Document> insertDocument(DocumentInput documentInput) {
        final String INSERT_DOCUMENT = "DocumentService.insertDocument()";
        log.debug(LogUtils.INVOKING_METHOD, INSERT_DOCUMENT, documentInput);

        final String DOCUMENT_INPUT = "DocumentInput";

        log.logChecking(DOCUMENT_INPUT);
        Document resp = new Document();
        if (documentInput == null) {
            String errorMsg = "Document is null";
            log.logCheckingOutcome(DOCUMENT_INPUT, false, errorMsg);
            throw new RepositoryManagerException(errorMsg);
        }
        if (documentInput.getDocumentKey() == null || documentInput.getDocumentKey().isBlank()) {
            String errorMsg = "Document Key is null";
            log.logCheckingOutcome(DOCUMENT_INPUT, false, errorMsg);
            throw new RepositoryManagerException(errorMsg);
        }
        log.logCheckingOutcome(DOCUMENT_INPUT, true);

        documentInput.setLastStatusChangeTimestamp(OffsetDateTime.now());
        String key = documentInput.getDocumentType();
        return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.getItem(Key.builder()
                                                                                    .partitionValue(documentInput.getDocumentKey())
                                                                                    .build()))
                   .handle((documentFounded, sink) -> {
                       if (documentFounded != null) {
                           log.debug("insertDocument() : document found : {}", documentFounded);
                           sink.error(new ItemAlreadyPresent(documentInput.getDocumentKey()));
                       }
                   })
                   .switchIfEmpty(Mono.just(documentInput))
                   .flatMap(o -> docTypesService.getDocType(key))
                   .flatMap(o -> {
                       resp.setDocumentType(o);
                       resp.setDocumentKey(documentInput.getDocumentKey());
                       resp.setDocumentState(documentInput.getDocumentState());
                       resp.setCheckSum(documentInput.getCheckSum());
                       resp.setRetentionUntil(documentInput.getRetentionUntil());
                       resp.setContentLenght(documentInput.getContentLenght());
                       resp.setContentType(documentInput.getContentType());
                       resp.setDocumentLogicalState(documentInput.getDocumentLogicalState());
                       resp.setClientShortCode(documentInput.getClientShortCode());
                       DocumentEntity documentEntityInput = objectMapper.convertValue(resp, DocumentEntity.class);
                       return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.putItem(builder -> builder.item(documentEntityInput)));
                   })
                      .doOnSuccess(unused -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, INSERT_DOCUMENT, resp))
                      .thenReturn(resp);
    }

    @Override
    public Mono<Document> patchDocument(String documentKey, DocumentChanges documentChanges, String authPagopaSafestorageCxId,
                                        String authApiKey) {
        final String PATCH_DOCUMENT = "DocumentService.patchDocument()";
        log.debug(LogUtils.INVOKING_METHOD, PATCH_DOCUMENT, Stream.of(documentKey, documentChanges, authPagopaSafestorageCxId).toList());

        // creo una variabile vuota per il vecchio stato in maniera thread-safe
        AtomicReference<String> oldState = new AtomicReference<>();

        // recupero il documento da Dynamo tramite la key, nel caso in cui non ci fosse, restituisco errore
        return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(documentKey).build()))
                .switchIfEmpty(getErrorIdDocNotFoundException(documentKey))
                .doOnError(DocumentKeyNotPresentException.class, throwable -> log.debug(throwable.getMessage()))
                .flatMap(documentEntity -> {
                    // confronto il documento con i cambiamenti, se sono uguali, restituisco il documento e avviso che la modifica è già stata apportata
                    if (hasBeenPatched(documentEntity, documentChanges)) {
                        log.debug("Same changes have been already applied to document '{}'", documentKey);
                        return Mono.just(documentEntity);
                    }
                    // altrimenti controllo che l'ultimo timestamp di cambiamento sia valorizzato
                    else if (documentChanges.getLastStatusChangeTimestamp() != null) {
                        // recupero l'ultimo timestamp valorizzato sul documento del db
                        var storedLastStatusChangeTimestamp = documentEntity.getLastStatusChangeTimestamp();
                        // e quello dall'oggetto che sto passando per il cambiamento
                        var lastStatusChangeTimestamp = documentChanges.getLastStatusChangeTimestamp();
                        // se il documento ha già una valorizzazione di cambio e questo è successivo a quello che gli sto passando
                        if (storedLastStatusChangeTimestamp!=null && lastStatusChangeTimestamp.isBefore(storedLastStatusChangeTimestamp))
                            return Mono.just(documentEntity);
                        // restituisco il documento originale e setto l'ultimo cambio stato a quello che gli sto passando
                        documentEntity.setLastStatusChangeTimestamp(lastStatusChangeTimestamp);
                    // se gli stati invece sono uguali
                    } else if (documentChanges.getDocumentState() != null && !documentChanges.getDocumentState().equals(documentEntity.getDocumentState())) {
                        // cambio il timestamp dell'ultimo cambiamento ad ora
                        documentEntity.setLastStatusChangeTimestamp(OffsetDateTime.now());
                    }
                    // eseguo la modifica
                    return executePatch(documentEntity, documentChanges, oldState, documentKey, authPagopaSafestorageCxId, authApiKey);
                })
                .map(documentEntity -> objectMapper.convertValue(documentEntity, Document.class));
    }

    private Mono<DocumentEntity> executePatch(DocumentEntity docEntity, DocumentChanges documentChanges, AtomicReference<String> oldState, String documentKey, String authPagopaSafestorageCxId, String authApiKey) {
        final String PATCH_DOCUMENT = "DocumentService.patchDocument()";

        return Mono.just(docEntity).flatMap(documentEntity ->
                {
                    // verifico che lo stato che passo per la modifica sia valorizzato
                    if (!StringUtils.isBlank(documentChanges.getDocumentState())) {
                        // creo un oggetto documentStatus Change e gli setto le possibili modifiche da dare in pasto alla macchina stati per la validazione
                        var documentStatusChange = new DocumentStatusChange();
                        documentStatusChange.setXPagopaExtchCxId(documentEntity.getClientShortCode());
                        documentStatusChange.setProcessId("SS");
                        documentStatusChange.setCurrentStatus(documentEntity.getDocumentState().toLowerCase());
                        documentStatusChange.setNextStatus(documentChanges.getDocumentState().toLowerCase());
                        // mi faccio validare il cmbio di stato dalla macchina stati facendomi ritornare una document Entity con le modifiche?
                        return callMacchinaStati.statusValidation(documentStatusChange).thenReturn(documentEntity);
                    } else {
                        return Mono.just(documentEntity);
                    }
                })
                .flatMap(documentEntityStored -> {
                    // mappo il documento ritornato, controllo se lo stato che gli passo è valorizzato
                    if (!StringUtils.isBlank(documentChanges.getDocumentState())) {
                        // il vecchio stato viene considerato nella gestione della retentionUntil
                        // se non è vuoto, setto la variabile old state in base allo stato del documento nel DB e la boolean statusFound a false
                        oldState.set(documentEntityStored.getDocumentState());
                        boolean statusFound = false;
                        // setto lo stato dell'oggetto con quello che viene passato dal client
                        documentEntityStored.setDocumentState(documentChanges.getDocumentState());

                    // controllo se lo stato del documento è valorizzato
                    if(documentEntityStored.getDocumentType().getStatuses() != null){
                        for (Map.Entry<String, CurrentStatusEntity> entry : documentEntityStored.getDocumentType().getStatuses().entrySet()) {
                            // se lo è, controllo per ciascuno degli stati se sono uguali a quello passato dal client
                            if (entry.getValue().getTechnicalState().equals(documentChanges.getDocumentState())) {
                                // se lo sono, setto lo stato logico attraverso la key della map e setto statusFound a true
                                documentEntityStored.setDocumentLogicalState(entry.getKey());
                                statusFound=true;
                                break;
                            }
                        }
                        // se status found è valorizzata a false, restituisco errore
                        if(!statusFound){
                            log.debug("New status inserted is invalid for the documentType, DocumentLogicalState was not updated");
                        }
                    } else {
                        // se lo stato che gli passo non è valorizzato, restituisco errore
                        String sMsg = "Cannot read statuses of Document cause statuses is null, therefore new status inserted is invalid";
                        throw new IllegalDocumentStateException(sMsg);
                    }
                }
                    // eseguo controlli su retention until passata dal client
                if (documentChanges.getRetentionUntil() != null && !documentChanges.getRetentionUntil().isBlank()) {
                    // se non è null né vuota, la setto al documento
                    documentEntityStored.setRetentionUntil(documentChanges.getRetentionUntil());
                }
                //eseguo controlli su checksum
                if (documentChanges.getCheckSum() != null) {
                    documentEntityStored.setCheckSum(documentChanges.getCheckSum());
                }
                // eseguo controlli su content lenght
                if (documentChanges.getContentLenght() != null) {
                    documentEntityStored.setContentLenght(documentChanges.getContentLenght());
                }
                log.debug("patchDocument() : (ho aggiornato documentEntity in base al documentChanges) documentEntity for patch = {}",
                        documentEntityStored);

                // se lo stato della modifica che sto passando è valorizzato come AVAILABLE o ATTACHED
                if ( documentChanges.getDocumentState() != null && (
                        documentChanges.getDocumentState().equalsIgnoreCase(Constant.AVAILABLE) ||
                                documentChanges.getDocumentState().equalsIgnoreCase(Constant.ATTACHED))) {

                    // istanzio variabile storaeType
                    String storageType;
                    // controllo se il tipo di documento nel db è diverso da null e ha stati valorizzati
                    if (documentEntityStored.getDocumentType() != null && documentEntityStored.getDocumentType().getStatuses() != null) {
                        // se il documento nel db ha uno stato che corrisponde al suo stesso stato logico
                        if (documentEntityStored.getDocumentType()
                                .getStatuses()
                                .containsKey(documentEntityStored.getDocumentLogicalState())) {
                            log.debug("patchDocument() : START Tagging");
                            // assegno alla variabile storagetype il valore dello storage di quello stato logico.
                            storageType = documentEntityStored.getDocumentType()
                                    .getStatuses()
                                    .get(documentEntityStored.getDocumentLogicalState())
                                    .getStorage();
                            Tagging tagging = Tagging.builder().tagSet(setTag -> {
                                setTag.key(STORAGE_TYPE);
                                setTag.value(storageType);
                            }).build();
                            return s3Service.putObjectTagging(documentKey, bucketName.ssHotName(), tagging)
                                    .thenReturn(documentEntityStored);
                        } else {
                            log.debug("patchDocument() : Tagging : storageTypeEmpty");
                        }
                    }
                }
                return Mono.just(documentEntityStored);
            })
            .flatMap(documentEntityStored -> {

                // verifico se nell richiesta di modifica la retention until è valorizzata o se lo stato è valorizzato come AVAILABLE o ATTACHED
                if (!StringUtils.isBlank(documentChanges.getRetentionUntil()) || (documentChanges.getDocumentState() != null && (
                        documentChanges.getDocumentState().equalsIgnoreCase(Constant.AVAILABLE) ||
                                documentChanges.getDocumentState().equalsIgnoreCase(Constant.ATTACHED)))) {

                    // setto i cambiamenti all'oggetto nel bucket
		                   return retentionService.setRetentionPeriodInBucketObjectMetadata(authPagopaSafestorageCxId,
		                           authApiKey,
		                           documentChanges,
		                           documentEntityStored,
		                           oldState.get());
                       }
                       else {
                    	   return Mono.just(documentEntityStored);
                       }
                   })
                   .flatMap(documentUpdated -> Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.updateItem(documentUpdated)))
                   .retryWhen(DYNAMO_OPTIMISTIC_LOCKING_RETRY)
                   .doOnSuccess(documentType -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, PATCH_DOCUMENT, documentChanges));
    }

    @Override
    public Mono<Document> deleteDocument(String documentKey) {
        final String DELETE_DOCUMENT = "DocumentService.deleteDocument()";
        log.debug(LogUtils.INVOKING_METHOD, DELETE_DOCUMENT, documentKey);
        Key typeKey = Key.builder().partitionValue(documentKey).build();

        return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.getItem(typeKey))
                   .switchIfEmpty(getErrorIdDocNotFoundException(documentKey))
                   .doOnError(DocumentKeyNotPresentException.class, throwable -> log.error("Error in DocumentServiceImpl.deleteDocument(): DocumentKeyNotPresentException - '{}'", throwable.getMessage()))
                   .zipWhen(documentToDelete -> Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.deleteItem(typeKey)))
                   .map(objects -> objectMapper.convertValue(objects.getT2(), Document.class))
                   .doOnSuccess(documentType -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, DELETE_DOCUMENT, documentType));
    }

    private boolean hasBeenPatched(DocumentEntity documentEntity, DocumentChanges documentChanges) {
        boolean hasBeenPatched = true;
        if (!Objects.isNull(documentChanges.getDocumentState())) {
            hasBeenPatched = documentChanges.getDocumentState().equalsIgnoreCase(documentEntity.getDocumentState());
        }
        if (!Objects.isNull(documentChanges.getRetentionUntil())) {
            hasBeenPatched = hasBeenPatched && Objects.equals(documentChanges.getRetentionUntil(), documentEntity.getRetentionUntil());
        }
        if (!Objects.isNull(documentChanges.getContentLenght())) {
            hasBeenPatched = hasBeenPatched && Objects.equals(documentChanges.getContentLenght(), documentEntity.getContentLenght());
        }
        if (!Objects.isNull(documentChanges.getCheckSum())) {
            hasBeenPatched = hasBeenPatched && Objects.equals(documentChanges.getCheckSum(), documentEntity.getCheckSum());
        }
        return hasBeenPatched;
    }

}

package it.pagopa.pnss.repositorymanager.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentChanges;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentInput;
import it.pagopa.pnss.common.constant.*;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.model.pojo.DocumentStatusChange;
import it.pagopa.pnss.common.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pnss.common.retention.RetentionService;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.s3.model.Tagging;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static it.pagopa.pnss.common.constant.Constant.STORAGE_TYPE;
import static it.pagopa.pnss.common.utils.DynamoDbUtils.DYNAMO_OPTIMISTIC_LOCKING_RETRY;


@Service
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final ObjectMapper objectMapper;
    private final DynamoDbAsyncTable<DocumentEntity> documentEntityDynamoDbAsyncTable;
    private final DocTypesService docTypesService;
    private final RetentionService retentionService;
    private final AwsConfigurationProperties awsConfigurationProperties;
    private final BucketName bucketName;
    private final CallMacchinaStati callMacchinaStati;
    private final S3Service s3Service;
    private final Retry dynamoRetryStrategy;
    @Autowired
    RepositoryManagerDynamoTableName managerDynamoTableName;

    public DocumentServiceImpl(ObjectMapper objectMapper, DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                               RepositoryManagerDynamoTableName repositoryManagerDynamoTableName, DocTypesService docTypesService,
                               RetentionService retentionService, AwsConfigurationProperties awsConfigurationProperties,
                               BucketName bucketName, CallMacchinaStati callMacchinaStati, S3Service s3Service, Retry dynamoRetryStrategy) {
        this.docTypesService = docTypesService;
        this.callMacchinaStati = callMacchinaStati;
        this.s3Service = s3Service;
        this.dynamoRetryStrategy = dynamoRetryStrategy;
        this.documentEntityDynamoDbAsyncTable = dynamoDbEnhancedAsyncClient.table(repositoryManagerDynamoTableName.documentiName(),
                                                                                  TableSchema.fromBean(DocumentEntity.class));
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
        final String GET_DOCUMENT = "DocumentServiceImpl.getDocument()";
        return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(documentKey).build()))
                   .retryWhen(dynamoRetryStrategy)
                   .switchIfEmpty(getErrorIdDocNotFoundException(documentKey))
                   .doOnError(DocumentKeyNotPresentException.class, throwable -> log.debug(throwable.getMessage()))
                   .map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, Document.class))
                   .doOnSuccess(documentType -> log.info(Constant.SUCCESSFUL_OPERATION_LABEL, documentKey, GET_DOCUMENT, documentType));
    }

    @Override
    public Mono<Document> insertDocument(DocumentInput documentInput) {
        final String DOCUMENT_INPUT = "documentInput in DocumentServiceImpl insertDocument()";

        log.info(Constant.CHECKING_VALIDATION_PROCESS, DOCUMENT_INPUT);
        Document resp = new Document();
        if (documentInput == null) {
            log.warn(Constant.VALIDATION_PROCESS_FAILED, DOCUMENT_INPUT, "Document is null");
            throw new RepositoryManagerException("Document is null");
        }
        if (documentInput.getDocumentKey() == null || documentInput.getDocumentKey().isBlank()) {
            log.warn(Constant.VALIDATION_PROCESS_FAILED, DOCUMENT_INPUT, "Document Key is null");
            throw new RepositoryManagerException("Document Key is null");
        }
        documentInput.setLastStatusChangeTimestamp(OffsetDateTime.now());

        log.info(Constant.VALIDATION_PROCESS_PASSED, DOCUMENT_INPUT);

        String key = documentInput.getDocumentType();
        return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.getItem(Key.builder()
                                                                                    .partitionValue(documentInput.getDocumentKey())
                                                                                    .build()))
                   .retryWhen(dynamoRetryStrategy)
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
                       log.debug(Constant.INSERTING_DATA_IN_DYNAMODB_TABLE, documentEntityInput, managerDynamoTableName.documentiName());
                       return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.putItem(builder -> builder.item(documentEntityInput)));
                   })
                    .doOnSuccess(unused -> {
                        log.info(Constant.INSERTED_DATA_IN_DYNAMODB_TABLE, managerDynamoTableName.documentiName());
                        log.info(Constant.SUCCESSFUL_OPERATION_LABEL, documentInput.getDocumentKey(), "DocumentServiceImpl.insertDocument()", resp);
                    })
                   .thenReturn(resp);
    }

    @Override
    public Mono<Document> patchDocument(String documentKey, DocumentChanges documentChanges, String authPagopaSafestorageCxId,
                                        String authApiKey) {

        AtomicReference<String> oldState = new AtomicReference<>();

        return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(documentKey).build()))
                .retryWhen(dynamoRetryStrategy)
                .switchIfEmpty(getErrorIdDocNotFoundException(documentKey))
                .doOnError(DocumentKeyNotPresentException.class, throwable -> log.debug(throwable.getMessage()))
                .flatMap(documentEntity -> {
                    if (hasBeenPatched(documentEntity, documentChanges)) {
                        log.debug("Same changes have been already applied to document '{}'", documentKey);
                        return Mono.just(documentEntity);
                    }
                    else if (documentChanges.getLastStatusChangeTimestamp() != null) {
                        var storedLastStatusChangeTimestamp = documentEntity.getLastStatusChangeTimestamp();
                        var lastStatusChangeTimestamp = documentChanges.getLastStatusChangeTimestamp();
                        if (storedLastStatusChangeTimestamp!=null && lastStatusChangeTimestamp.isBefore(storedLastStatusChangeTimestamp))
                            return Mono.just(documentEntity);
                        documentEntity.setLastStatusChangeTimestamp(lastStatusChangeTimestamp);
                    } else if (documentChanges.getDocumentState() != null && !documentChanges.getDocumentState().equals(documentEntity.getDocumentState())) {
                        documentEntity.setLastStatusChangeTimestamp(OffsetDateTime.now());
                    }
                    return executePatch(documentEntity, documentChanges, oldState, documentKey, authPagopaSafestorageCxId, authApiKey);
                })
                .map(documentEntity -> objectMapper.convertValue(documentEntity, Document.class));
    }

    private Mono<DocumentEntity> executePatch(DocumentEntity docEntity, DocumentChanges documentChanges, AtomicReference<String> oldState, String documentKey, String authPagopaSafestorageCxId, String authApiKey) {
       final String DOCUMENT_STATE = "documentState in DocumentServiceImpl.patchDocument()";

       log.info(Constant.CHECKING_VALIDATION_PROCESS, DOCUMENT_STATE);
        return Mono.just(docEntity).flatMap(documentEntity ->
                {
                    if (!StringUtils.isBlank(documentChanges.getDocumentState())) {
                        var documentStatusChange = new DocumentStatusChange();
                        documentStatusChange.setXPagopaExtchCxId(documentEntity.getClientShortCode());
                        documentStatusChange.setProcessId("SS");
                        documentStatusChange.setCurrentStatus(documentEntity.getDocumentState().toLowerCase());
                        documentStatusChange.setNextStatus(documentChanges.getDocumentState().toLowerCase());
                        log.info(Constant.VALIDATION_PROCESS_PASSED, DOCUMENT_STATE);
                        log.info(Constant.INVOKING_EXTERNAL_SERVICE, "pn-statemachinemanager statusValidation");
                        return callMacchinaStati.statusValidation(documentStatusChange).thenReturn(documentEntity);
                    } else {
                        return Mono.just(documentEntity);
                    }
                })
                .flatMap(documentEntityStored -> {
                    log.debug("patchDocument() : (recupero documentEntity dal DB) documentEntityStored = {}", documentEntityStored);
                    if (!StringUtils.isBlank(documentChanges.getDocumentState())) {
                        // il vecchio stato viene considerato nella gestione della retentionUntil
                        oldState.set(documentEntityStored.getDocumentState());
                        boolean statusFound = false;
                        documentEntityStored.setDocumentState(documentChanges.getDocumentState());

                    if(documentEntityStored.getDocumentType().getStatuses() != null){
                        for (Map.Entry<String, CurrentStatusEntity> entry : documentEntityStored.getDocumentType().getStatuses().entrySet()) {
                            if (entry.getValue().getTechnicalState().equals(documentChanges.getDocumentState())) {
                                documentEntityStored.setDocumentLogicalState(entry.getKey());
                                statusFound=true;
                                break;
                            }
                        }
                        if(!statusFound){
                            log.debug("New status inserted is invalid for the documentType, DocumentLogicalState was not updated");
                        }
                    } else {
                        String sMsg = "Cannot read statuses of Document cause statuses is null, therefore new status inserted is invalid";
                        throw new IllegalDocumentStateException(sMsg);
                    }
                }
                if (documentChanges.getRetentionUntil() != null && !documentChanges.getRetentionUntil().isBlank()) {
                    documentEntityStored.setRetentionUntil(documentChanges.getRetentionUntil());
                }
                if (documentChanges.getCheckSum() != null) {
                    documentEntityStored.setCheckSum(documentChanges.getCheckSum());
                }
                if (documentChanges.getContentLenght() != null) {
                    documentEntityStored.setContentLenght(documentChanges.getContentLenght());
                }
                log.debug("patchDocument() : (ho aggiornato documentEntity in base al documentChanges) documentEntity for patch = {}",
                        documentEntityStored);


                if ( documentChanges.getDocumentState() != null && (
                        documentChanges.getDocumentState().equalsIgnoreCase(Constant.AVAILABLE) ||
                                documentChanges.getDocumentState().equalsIgnoreCase(Constant.ATTACHED))) {

                    String storageType;
                    if (documentEntityStored.getDocumentType() != null && documentEntityStored.getDocumentType().getStatuses() != null) {
                        if (documentEntityStored.getDocumentType()
                                .getStatuses()
                                .containsKey(documentEntityStored.getDocumentLogicalState())) {
                            log.debug("patchDocument() : START Tagging");
                            storageType = documentEntityStored.getDocumentType()
                                    .getStatuses()
                                    .get(documentEntityStored.getDocumentLogicalState())
                                    .getStorage();
                            Tagging tagging = Tagging.builder().tagSet(setTag -> {
                                setTag.key(STORAGE_TYPE);
                                setTag.value(storageType);
                            }).build();
                            log.info(Constant.CLIENT_METHOD_INVOCATION, "s3Service.putObjectTagging()", tagging);
                            log.debug("patchDocument() : Tagging : storageType {}", storageType);
                            return s3Service.putObjectTagging(documentKey, bucketName.ssHotName(), tagging)
                                    .doOnSuccess(result -> log.debug("patchDocument() : END Tagging"))
                                    .thenReturn(documentEntityStored);
                        } else {
                            log.debug("patchDocument() : Tagging : storageTypeEmpty");
                        }
                    }
                }
                return Mono.just(documentEntityStored);
            })
            .flatMap(documentEntityStored -> {

                if (!StringUtils.isBlank(documentChanges.getRetentionUntil()) || (documentChanges.getDocumentState() != null && (
                        documentChanges.getDocumentState().equalsIgnoreCase(Constant.AVAILABLE) ||
                                documentChanges.getDocumentState().equalsIgnoreCase(Constant.ATTACHED)))) {

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
                   .flatMap(documentUpdated -> {
                       log.debug(Constant.UPDATING_DATA_IN_DYNAMODB_TABLE, documentUpdated, managerDynamoTableName.documentiName());
                       CompletableFuture<DocumentEntity> document = documentEntityDynamoDbAsyncTable.updateItem(documentUpdated);
                       log.info(Constant.UPDATED_DATA_IN_DYNAMODB_TABLE, managerDynamoTableName.documentiName());
                       return Mono.fromCompletionStage(document);
                   })
                   .retryWhen(dynamoRetryStrategy)
                   .doOnSuccess(documentType -> log.info(Constant.SUCCESSFUL_OPERATION_LABEL, documentKey, "DocumentServiceImpl.patchDocument()", documentChanges));
    }

    @Override
    public Mono<Document> deleteDocument(String documentKey) {
        Key typeKey = Key.builder().partitionValue(documentKey).build();

        return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.getItem(typeKey))
                   .retryWhen(dynamoRetryStrategy)
                   .switchIfEmpty(getErrorIdDocNotFoundException(documentKey))
                   .doOnError(DocumentKeyNotPresentException.class, throwable -> log.error("Error in DocumentServiceImpl.deleteDocument(): DocumentKeyNotPresentException - '{}'", throwable.getMessage()))
                   .zipWhen(documentToDelete -> {
                       log.debug(Constant.DELETING_DATA_IN_DYNAMODB_TABLE, typeKey, managerDynamoTableName.documentiName());
                       return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.deleteItem(typeKey));
                   })
                   .retryWhen(dynamoRetryStrategy)
                   .doOnSuccess(unused -> log.info(Constant.DELETED_DATA_IN_DYNAMODB_TABLE, managerDynamoTableName.documentiName()))
                   .map(objects -> objectMapper.convertValue(objects.getT2(), Document.class))
                   .doOnSuccess(documentType -> log.info(Constant.SUCCESSFUL_OPERATION_LABEL, documentKey, "DocumentServiceImpl.deleteDocument()", documentType));
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

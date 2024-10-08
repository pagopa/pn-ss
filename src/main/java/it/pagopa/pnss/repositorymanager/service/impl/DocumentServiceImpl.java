package it.pagopa.pnss.repositorymanager.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Document;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentInput;
import it.pagopa.pnss.common.constant.*;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.model.pojo.DocumentStatusChange;
import it.pagopa.pnss.common.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pnss.common.retention.RetentionService;
import it.pagopa.pnss.common.service.IgnoredUpdateMetadataHandler;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configurationproperties.AwsConfigurationProperties;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.CurrentStatusEntity;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.repositorymanager.exception.IllegalDocumentStateException;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.exception.ResourceDeletedException;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import it.pagopa.pnss.repositorymanager.service.DocumentService;
import it.pagopa.pnss.transformation.service.S3Service;
import lombok.CustomLog;
import org.apache.tika.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.constant.Constant.*;


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
    private final IgnoredUpdateMetadataHandler ignoredUpdateMetadataHandler;

    public DocumentServiceImpl(ObjectMapper objectMapper, DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                               RepositoryManagerDynamoTableName repositoryManagerDynamoTableName, DocTypesService docTypesService,
                               RetentionService retentionService, AwsConfigurationProperties awsConfigurationProperties,
                               BucketName bucketName, CallMacchinaStati callMacchinaStati, S3Service s3Service,
                               IgnoredUpdateMetadataHandler ignoredUpdateMetadataHandler) {
        this.docTypesService = docTypesService;
        this.callMacchinaStati = callMacchinaStati;
        this.s3Service = s3Service;
        this.ignoredUpdateMetadataHandler = ignoredUpdateMetadataHandler;
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

        AtomicReference<String> oldState = new AtomicReference<>();

        return Mono.defer(()-> Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(documentKey).build())))
                .switchIfEmpty(getErrorIdDocNotFoundException(documentKey))
                .doOnError(DocumentKeyNotPresentException.class, throwable -> log.debug(throwable.getMessage()))
                .handle((documentEntity, sink) -> {
                    if (documentEntity.getDocumentState().equals(DELETED)) {
                        sink.error(new ResourceDeletedException.DocumentDeletedException(documentKey));
                    } else sink.next(documentEntity);
                })
                .cast(DocumentEntity.class)
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
        final String PATCH_DOCUMENT = "DocumentService.patchDocument()";
        return Mono.just(docEntity).flatMap(documentEntity ->
                {
                    if (!StringUtils.isBlank(documentChanges.getDocumentState())) {
                        var documentStatusChange = new DocumentStatusChange();
                        documentStatusChange.setXPagopaExtchCxId(documentEntity.getClientShortCode());
                        documentStatusChange.setProcessId("SS");
                        documentStatusChange.setCurrentStatus(documentEntity.getDocumentState().toLowerCase());
                        documentStatusChange.setNextStatus(documentChanges.getDocumentState().toLowerCase());
                        return callMacchinaStati.statusValidation(documentStatusChange).thenReturn(documentEntity);
                    } else {
                        return Mono.just(documentEntity);
                    }
                })
                .transform(checkIfFileToIgnoreExistsInS3())
                .flatMap(documentEntityStored -> {
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
                    if (!ignoredUpdateMetadataHandler.isToIgnore(documentKey) && documentEntityStored.getDocumentType() != null && documentEntityStored.getDocumentType().getStatuses() != null) {
                        if (documentEntityStored.getDocumentType()
                                .getStatuses()
                                .containsKey(documentEntityStored.getDocumentLogicalState())) {
                            log.debug("patchDocument() : START Tagging");
                            storageType = documentEntityStored.getDocumentType()
                                    .getStatuses()
                                    .get(documentEntityStored.getDocumentLogicalState())
                                    .getStorage();
                            Tag expiryTag = Tag.builder().key(STORAGE_EXPIRY).value(storageType).build();
                            Tag freezeTag = Tag.builder().key(STORAGE_FREEZE).value(storageType).build();
                            Tagging tagging = Tagging.builder().tagSet(expiryTag, freezeTag).build();
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

                if (!ignoredUpdateMetadataHandler.isToIgnore(documentKey) &&
                        (!StringUtils.isBlank(documentChanges.getRetentionUntil()) || (documentChanges.getDocumentState() != null &&
                                (documentChanges.getDocumentState().equalsIgnoreCase(Constant.AVAILABLE) ||
                                documentChanges.getDocumentState().equalsIgnoreCase(Constant.ATTACHED))))) {

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
                   .doOnSuccess(documentType -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, PATCH_DOCUMENT, documentChanges));
    }

    //Related to the IgnoredUpdateMetadataHandler. It checks if the file to ignore exists in S3.
    //If it does exist, removes it from the list of file keys to ignore.
    private Function<Mono<DocumentEntity>, Mono<DocumentEntity>> checkIfFileToIgnoreExistsInS3() {
        return tMono -> tMono.flatMap(documentEntity -> {
            String documentKey = documentEntity.getDocumentKey();
            boolean isToIgnore = ignoredUpdateMetadataHandler.isToIgnore(documentKey);
            if (isToIgnore) {
                return s3Service.headObject(documentKey, bucketName.ssHotName()).onErrorResume(NoSuchKeyException.class, throwable -> {
                    log.debug("File with key '{}' is to ignore", documentKey);
                    return Mono.empty();
                }).doOnNext(headObjectResponse -> {
                    log.warn("File to ignore with key '{}' exists in S3 bucket.", documentKey);
                    ignoredUpdateMetadataHandler.removeFileKey(documentKey);
                }).thenReturn(documentEntity);
            } else return Mono.just(documentEntity);
        });
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

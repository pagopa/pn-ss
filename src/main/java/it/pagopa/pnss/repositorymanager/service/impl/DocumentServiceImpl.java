package it.pagopa.pnss.repositorymanager.service.impl;

import com.amazonaws.services.lambda.runtime.events.ConnectEvent;
import com.amazonaws.services.s3.model.ObjectTagging;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentInput;
import it.pagopa.pnss.configurationproperties.AwsConfigurationProperties;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.repositorymanager.exception.IllegalDocumentStateException;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentChanges;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.Tagging;

import java.util.concurrent.CompletableFuture;

import static it.pagopa.pnss.common.Constant.STORAGETYPE;


@Service
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final ObjectMapper objectMapper;

    private final AwsConfigurationProperties awsConfigurationProperties;

    private final BucketName bucketName;

    private final DynamoDbAsyncTable<DocumentEntity> documentEntityDynamoDbAsyncTable;

    private final DocTypesService docTypesService;

    public DocumentServiceImpl(ObjectMapper objectMapper, DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                               RepositoryManagerDynamoTableName repositoryManagerDynamoTableName, DocTypesService docTypesService, AwsConfigurationProperties awsConfigurationProperties, BucketName bucketName) {
        this.docTypesService = docTypesService;
        this.bucketName = bucketName;
        this.documentEntityDynamoDbAsyncTable = dynamoDbEnhancedAsyncClient.table(repositoryManagerDynamoTableName.documentiName(),
                                                                                  TableSchema.fromBean(DocumentEntity.class));
        this.objectMapper = objectMapper;
        this.awsConfigurationProperties = awsConfigurationProperties;
    }

    private Mono<DocumentEntity> getErrorIdDocNotFoundException(String documentKey) {
        log.error("getErrorIdDocNotFoundException() : document with documentKey \"{}\" not found", documentKey);
        return Mono.error(new DocumentKeyNotPresentException(documentKey));
    }

    @Override
    public Mono<Document> getDocument(String documentKey) {
        log.info("getDocument() : IN : documentKey {}", documentKey);
        return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(documentKey).build()))
                   .switchIfEmpty(getErrorIdDocNotFoundException(documentKey))
                   .doOnError(DocumentKeyNotPresentException.class, throwable -> log.error(throwable.getMessage()))
                   .map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, Document.class));
    }

    @Override
    public Mono<Document> insertDocument(DocumentInput documentInput) {
        log.info("insertDocument() : IN : documentInput : {}", documentInput);
        Document resp = new Document();
        if (documentInput == null) {
            throw new RepositoryManagerException("Document is null");
        }
        if (documentInput.getDocumentKey() == null || documentInput.getDocumentKey().isBlank()) {
            throw new RepositoryManagerException("Document Key is null");
        }
        String key = documentInput.getDocumentType();
        return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.getItem(Key.builder()
                        .partitionValue(documentInput.getDocumentKey())
                        .build()))
                .handle((documentFounded, sink) -> {
                            if (documentFounded != null) {
                                log.error("insertDocument() : document founded : {}", documentFounded);
                                sink.error(new ItemAlreadyPresent(documentInput.getDocumentKey()));
                            }
                        }
                )
                .doOnError(ItemAlreadyPresent.class, throwable -> log.error(throwable.getMessage()))
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
                    DocumentEntity documentEntityInput = objectMapper.convertValue(resp, DocumentEntity.class);
                    return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.putItem(builder -> builder.item(
                            documentEntityInput)));
                })
                .thenReturn(resp);
    }

    @Override
    public Mono<Document> patchDocument(String documentKey, DocumentChanges documentChanges) {
        log.info("patchDocument() : IN : documentKey : {} , documentChanges {}", documentKey, documentChanges);
        
        return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(documentKey).build()))
                   .switchIfEmpty(getErrorIdDocNotFoundException(documentKey))
                   .doOnError(DocumentKeyNotPresentException.class, throwable -> log.error(throwable.getMessage()))
                   .map(documentEntityStored -> {
                	   if (documentChanges == null) {
                		   return documentEntityStored;
                	   }
                       log.info("patchDocument() : documentEntityStored : {}", documentEntityStored);
                       if (documentChanges.getDocumentState() != null) {
                           documentEntityStored.setDocumentState(documentChanges.getDocumentState());
                       }
                       if(documentChanges.getDocumentState().equalsIgnoreCase("available")) {
                           if(documentEntityStored.getDocumentType().getTipoDocumento().equalsIgnoreCase("PN_NOTIFICATION_ATTACHMENTS")) {
                               documentEntityStored.setDocumentLogicalState("PRELOADED");
                           } else {
                               documentEntityStored.setDocumentLogicalState("SAVED");
                           }
                       }
                       if(documentChanges.getDocumentState().equalsIgnoreCase("attached")) {
                           if(documentEntityStored.getDocumentType().getTipoDocumento().equalsIgnoreCase("PN_NOTIFICATION_ATTACHMENTS")) {
                               documentEntityStored.setDocumentLogicalState("ATTACHED");
                           } else {
                               throw new IllegalDocumentStateException("Document State inserted is invalid for present document type");
                           }
                       }
                       if (documentChanges.getRetentionUntil() != null && documentChanges.getRetentionUntil().isBlank()) {
                    	   documentEntityStored.setRetentionUntil(documentChanges.getRetentionUntil());
                       }
                       if (documentChanges.getCheckSum() != null) {
                    	   documentEntityStored.setCheckSum(documentChanges.getCheckSum());
                       }
                       if (documentChanges.getContentLenght() != null) {
                    	   documentEntityStored.setContentLenght(documentChanges.getContentLenght());
                       }
                       log.info("patchDocument() : documentEntity for patch : {}", documentEntityStored);

                       log.info("patchDocument() : start Tagging");
                       Region region = Region.of(awsConfigurationProperties.regionCode());
                       S3AsyncClient s3 = S3AsyncClient.builder()
                               .region(region)
                               .build();
                       String storageType;
                       PutObjectTaggingRequest putObjectTaggingRequest;
                       if(documentEntityStored.getDocumentType().getStatuses().containsKey(documentEntityStored.getDocumentLogicalState())){
                           storageType =  documentEntityStored.getDocumentType().getStatuses().get(documentEntityStored.getDocumentLogicalState()).getStorage();
                            putObjectTaggingRequest = PutObjectTaggingRequest.builder()
                                    .bucket(bucketName.ssHotName())
                                   .key(documentKey)
                                   .tagging(taggingBuilder -> taggingBuilder.tagSet(setTag -> {
                                       setTag.key(STORAGETYPE);
                                       setTag.value(storageType);
                                   }))
                                   .build();
                           CompletableFuture<PutObjectTaggingResponse> putObjectTaggingResponse = s3.putObjectTagging(putObjectTaggingRequest);
                           log.info("patchDocument() : Tagging : storageType {}", storageType);
                       } else {
                           log.info("patchDocument() : Tagging : storageTypeEmpty");
                       }
                       log.info("patchDocument() : end Tagging");
                       return documentEntityStored;
                   })
                   .doOnError(IllegalArgumentException.class, throwable -> log.error(throwable.getMessage()))
                   .zipWhen(documentUpdated -> Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.updateItem(documentUpdated)))
                   .map(objects -> objectMapper.convertValue(objects.getT2(), Document.class));
    }

    @Override
    public Mono<Document> deleteDocument(String documentKey) {
        log.info("deleteDocument() : IN : documentKey {}", documentKey);
        Key typeKey = Key.builder().partitionValue(documentKey).build();

        return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.getItem(typeKey))
                   .switchIfEmpty(getErrorIdDocNotFoundException(documentKey))
                   .doOnError(DocumentKeyNotPresentException.class, throwable -> log.error(throwable.getMessage()))
                   .zipWhen(documentToDelete -> Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.deleteItem(typeKey)))
                   .map(objects -> objectMapper.convertValue(objects.getT2(), Document.class));
    }
}

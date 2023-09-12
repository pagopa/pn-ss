package it.pagopa.pnss.repositorymanager.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentChanges;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentInput;
import it.pagopa.pnss.common.constant.*;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.model.dto.MacchinaStatiValidateStatoResponseDto;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.utils.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingResponse;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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

    public DocumentServiceImpl(ObjectMapper objectMapper, DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                               RepositoryManagerDynamoTableName repositoryManagerDynamoTableName, DocTypesService docTypesService,
                               RetentionService retentionService, AwsConfigurationProperties awsConfigurationProperties,
                               BucketName bucketName, CallMacchinaStati callMacchinaStati) {
        this.docTypesService = docTypesService;
        this.callMacchinaStati = callMacchinaStati;
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
        String decodedDocumentKey = URLDecoder.decode(documentKey, StandardCharsets.UTF_8);
        log.info("getDocument() : IN : documentKey {}", decodedDocumentKey);
        return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(decodedDocumentKey).build()))
                   .switchIfEmpty(getErrorIdDocNotFoundException(decodedDocumentKey))
                   .doOnError(DocumentKeyNotPresentException.class, throwable -> log.debug(throwable.getMessage()))
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
        String decodedDocumentKey = URLDecoder.decode(documentInput.getDocumentKey(), StandardCharsets.UTF_8);
        String key = documentInput.getDocumentType();
        return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.getItem(Key.builder()
                                                                                    .partitionValue(decodedDocumentKey)
                                                                                    .build()))
                   .handle((documentFounded, sink) -> {
                       if (documentFounded != null) {
                           log.debug("insertDocument() : document found : {}", documentFounded);
                           sink.error(new ItemAlreadyPresent(decodedDocumentKey));
                       }
                   })
                   .switchIfEmpty(Mono.just(documentInput))
                   .flatMap(o -> docTypesService.getDocType(key))
                   .flatMap(o -> {
                       resp.setDocumentType(o);
                       resp.setDocumentKey(decodedDocumentKey);
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
                   .thenReturn(resp);
    }

    @Override
    public Mono<Document> patchDocument(String documentKey, DocumentChanges documentChanges, String authPagopaSafestorageCxId,
                                        String authApiKey) {
        String decodedDocumentKey = URLDecoder.decode(documentKey, StandardCharsets.UTF_8);
        log.info("patchDocument() : START : authPagopaSafestorageCxId = {} : authApiKey = {} : documentKey = {} : documentChanges  = {}",
                 authPagopaSafestorageCxId,
                 authApiKey,
                 decodedDocumentKey,
                 documentChanges);

        AtomicReference<String> oldState = new AtomicReference<>();

        return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(decodedDocumentKey).build()))
                   .switchIfEmpty(getErrorIdDocNotFoundException(decodedDocumentKey))
                   .doOnError(DocumentKeyNotPresentException.class, throwable -> log.debug(throwable.getMessage()))
                   .zipWhen(documentEntity -> {

                       if(!StringUtils.isBlank(documentChanges.getDocumentState())) {
                           var documentStatusChange = new DocumentStatusChange();
                           documentStatusChange.setXPagopaExtchCxId(documentEntity.getClientShortCode());
                           documentStatusChange.setProcessId("SS");
                           documentStatusChange.setCurrentStatus(documentEntity.getDocumentState().toLowerCase());
                           documentStatusChange.setNextStatus(documentChanges.getDocumentState().toLowerCase());
                           return callMacchinaStati.statusValidation(documentStatusChange);
                       } else {
                           return Mono.just(new MacchinaStatiValidateStatoResponseDto());
                       }
                   })
                   .map(tuple -> {
                       DocumentEntity documentEntityStored = tuple.getT1();
                       log.debug("patchDocument() : (recupero documentEntity dal DB) documentEntityStored = {}", documentEntityStored);
                       if (!StringUtils.isBlank(documentChanges.getDocumentState())) {
                           // il vecchio stato viene considerato nella gestione della retentionUntil
                           oldState.set(documentEntityStored.getDocumentState());
                           boolean statusFound= false;
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
                    		   documentChanges.getDocumentState().toUpperCase().equals(Constant.AVAILABLE) ||
                    		   documentChanges.getDocumentState().toUpperCase().equals(Constant.ATTACHED))) {

	                       log.debug("patchDocument() : START Tagging");
	                       Region region = Region.of(awsConfigurationProperties.regionCode());
	                       S3AsyncClient s3 = S3AsyncClient.builder().region(region).build();
	                       String storageType;
	                       PutObjectTaggingRequest putObjectTaggingRequest;
	                       if (documentEntityStored.getDocumentType() != null && documentEntityStored.getDocumentType().getStatuses() != null) {
	                           if (documentEntityStored.getDocumentType()
	                                                   .getStatuses()
	                                                   .containsKey(documentEntityStored.getDocumentLogicalState())) {
	                               storageType = documentEntityStored.getDocumentType()
	                                                                 .getStatuses()
	                                                                 .get(documentEntityStored.getDocumentLogicalState())
	                                                                 .getStorage();
	                               putObjectTaggingRequest = PutObjectTaggingRequest.builder()
	                                                                                .bucket(bucketName.ssHotName())
	                                                                                .key(decodedDocumentKey)
	                                                                                .tagging(taggingBuilder -> taggingBuilder.tagSet(setTag -> {
	                                                                                    setTag.key(STORAGE_TYPE);
	                                                                                    setTag.value(storageType);
	                                                                                }))
	                                                                                .build();
	                               CompletableFuture<PutObjectTaggingResponse> putObjectTaggingResponse =
	                                       s3.putObjectTagging(putObjectTaggingRequest);
	                               log.debug("patchDocument() : Tagging : storageType {}", storageType);
	                           } else {
	                               log.debug("patchDocument() : Tagging : storageTypeEmpty");
	                           }
	                           log.debug("patchDocument() : END Tagging");
	                       }
                       }
                       return documentEntityStored;
                   })
                   .flatMap(documentEntityStored -> { 

                       if ( documentChanges.getDocumentState() != null && (
                    		   documentChanges.getDocumentState().toUpperCase().equals(Constant.AVAILABLE) ||
                    		   documentChanges.getDocumentState().toUpperCase().equals(Constant.ATTACHED))) {

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
                   .zipWhen(documentUpdated -> Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.updateItem(documentUpdated)))
                   .retryWhen(DYNAMO_OPTIMISTIC_LOCKING_RETRY)
                   .map(objects -> objectMapper.convertValue(objects.getT2(), Document.class));
    }

    @Override
    public Mono<Document> deleteDocument(String documentKey) {
        String decodedDocumentKey = URLDecoder.decode(documentKey, StandardCharsets.UTF_8);
        log.info("deleteDocument() : IN : documentKey {}", decodedDocumentKey);
        Key typeKey = Key.builder().partitionValue(decodedDocumentKey).build();

        return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.getItem(typeKey))
                   .switchIfEmpty(getErrorIdDocNotFoundException(decodedDocumentKey))
                   .doOnError(DocumentKeyNotPresentException.class, throwable -> log.debug(throwable.getMessage()))
                   .zipWhen(documentToDelete -> Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.deleteItem(typeKey)))
                   .map(objects -> objectMapper.convertValue(objects.getT2(), Document.class));
    }

}

package it.pagopa.pnss.repositorymanager.service.impl;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentInput;
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

@Service
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final ObjectMapper objectMapper;

    private final DynamoDbAsyncTable<DocumentEntity> documentEntityDynamoDbAsyncTable;

    private final DocTypesService docTypesService;

    public DocumentServiceImpl(ObjectMapper objectMapper, DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                               RepositoryManagerDynamoTableName repositoryManagerDynamoTableName, DocTypesService docTypesService) {
        this.docTypesService = docTypesService;
        this.documentEntityDynamoDbAsyncTable = dynamoDbEnhancedAsyncClient.table(repositoryManagerDynamoTableName.documentiName(),
                                                                                  TableSchema.fromBean(DocumentEntity.class));
        this.objectMapper = objectMapper;
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
                   })
                .flatMap(o -> docTypesService.getDocType(key))
                .flatMap(o -> {
                    Document docImp = new Document();
                    docImp.setDocumentType(o);
                    docImp.setDocumentKey(documentInput.getDocumentKey());
                       docImp.setDocumentState(objectMapper.convertValue(documentInput.getDocumentState(),Document.DocumentStateEnum.class));
                       docImp.setCheckSum(objectMapper.convertValue(documentInput.getCheckSum(),Document.CheckSumEnum.class));
                    docImp.setRetentionUntil(documentInput.getRetentionUntil());
                    docImp.setContentLenght(documentInput.getContentLenght());
                    docImp.setContentType(documentInput.getContentType());


                    DocumentEntity documentEntityInput = objectMapper.convertValue(docImp, DocumentEntity.class);

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
                           documentEntityStored.setDocumentState(Document.DocumentStateEnum.fromValue(documentChanges.getDocumentState().getValue()));
                       }
                       if (documentChanges.getRetentionUntil() != null && documentChanges.getRetentionUntil().isBlank()) {
                    	   documentEntityStored.setRetentionUntil(documentChanges.getRetentionUntil());
                       }
                       if (documentChanges.getCheckSum() != null) {
                    	   documentEntityStored.setCheckSum(documentChanges.getCheckSum().getValue());
                       }
                       if (documentChanges.getContentLenght() != null) {
                    	   documentEntityStored.setContentLenght(documentChanges.getContentLenght());
                       }
                       log.info("patchDocument() : documentEntity for patch : {}", documentEntityStored);
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

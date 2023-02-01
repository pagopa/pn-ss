package it.pagopa.pnss.repositoryManager.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.constant.DynamoTableNameConstant;
import it.pagopa.pnss.repositoryManager.entity.DocumentEntity;
import it.pagopa.pnss.repositoryManager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositoryManager.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Service
@Slf4j
public class DocumentServiceImpl implements DocumentService {
	
	@Autowired
	private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
	@Autowired
    private ObjectMapper objectMapper;
    
	@Override
	public Mono<Document> getDocument(String documentKey) {
		
        DynamoDbAsyncTable<DocumentEntity> docTypesTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.DOCUMENT_TABLE_NAME, TableSchema.fromBean(DocumentEntity.class));
        
        Mono<DocumentEntity> monoEntity = Mono.fromCompletionStage(docTypesTable.getItem(Key.builder().partitionValue(documentKey).build()))
        			.switchIfEmpty(Mono.error(new IdClientNotFoundException(documentKey)))
        			.doOnError(IdClientNotFoundException.class, throwable -> log.info(throwable.getMessage()));
        
        return monoEntity.map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, Document.class));
	}
	
	@Override
	public Mono<Document> insertDocument(Document documentInput) {
		
        DynamoDbAsyncTable<DocumentEntity> documentTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.DOCUMENT_TABLE_NAME, TableSchema.fromBean(DocumentEntity.class));
        DocumentEntity documentEntityInput = objectMapper.convertValue(documentInput, DocumentEntity.class);
        
        Mono<DocumentEntity> monoEntity = Mono.fromCompletionStage(documentTable.getItem(Key.builder().partitionValue(documentInput.getDocumentKey()).build()))
        	.handle( (documentFounded, sink) -> {
        		if (documentFounded != null) {
        			sink.error(new ItemAlreadyPresent(documentInput.getDocumentKey()));
        		}
        	})
        	.doOnError(ItemAlreadyPresent.class, throwable -> log.info(throwable.getMessage()))
        	.doOnSuccess(unused -> documentTable.putItem(builder -> builder.item(documentEntityInput)))
        	.thenReturn(documentEntityInput);
        
        return monoEntity.map(documentEntity -> objectMapper.convertValue(documentEntity, Document.class));
	}
	
	@Override
	public Mono<Document> patchDocument(String documentKey, Document documentInput) {
		
        DynamoDbAsyncTable<DocumentEntity> documentTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.DOCUMENT_TABLE_NAME, TableSchema.fromBean(DocumentEntity.class));
        DocumentEntity documentEntityInput = objectMapper.convertValue(documentInput, DocumentEntity.class);

        Mono<DocumentEntity> monoEntity = Mono.fromCompletionStage(documentTable.getItem(Key.builder().partitionValue(documentKey).build()))
        		.switchIfEmpty(Mono.error(new IdClientNotFoundException(documentKey)))
        		.doOnError(IdClientNotFoundException.class, throwable -> log.info(throwable.getMessage()))
                .doOnSuccess(documentEntityStored -> {
                	documentEntityInput.setDocumentKey(documentKey);
                	// aggiorno solo lo stato
                	if (documentEntityInput.getDocumentState() == null) {
                		documentEntityInput.setDocumentState(documentEntityStored.getDocumentState());
                	}
                	documentEntityInput.setRetentionPeriod(documentEntityStored.getRetentionPeriod());
                	documentEntityInput.setCheckSum(documentEntityStored.getCheckSum());
                	documentEntityInput.setContentLenght(documentEntityStored.getContentLenght());
                	documentEntityInput.setContentType(documentEntityStored.getContentType());
                	documentEntityInput.setDocumentType(documentEntityStored.getDocumentType());
                	// Updates an item in the mapped table, or adds it if it doesn't exist. 
                	documentTable.updateItem(documentEntityInput);
                })
                .thenReturn(documentEntityInput);
        
        return monoEntity.map(documentEntity -> objectMapper.convertValue(documentEntity, Document.class));
    	
    }

	@Override
	public Mono<Document> deleteDocument(String documentKey) {

        DynamoDbAsyncTable<DocumentEntity> documentTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.DOCUMENT_TABLE_NAME, TableSchema.fromBean(DocumentEntity.class));
        Key typeKey = Key.builder().partitionValue(documentKey).build();
        
        return Mono.fromCompletionStage(documentTable.getItem(typeKey))
        		.switchIfEmpty(Mono.error(new IdClientNotFoundException(documentKey)))
        		.doOnError(IdClientNotFoundException.class, throwable -> log.info(throwable.getMessage()))
        		.doOnSuccess(unused -> documentTable.deleteItem(typeKey))
        		.map(documentEntity -> objectMapper.convertValue(documentEntity, Document.class));
    }
}

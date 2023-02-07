package it.pagopa.pnss.repositoryManager.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.constant.DynamoTableNameConstant;
import it.pagopa.pnss.repositoryManager.entity.DocumentEntity;
import it.pagopa.pnss.repositoryManager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositoryManager.exception.RepositoryManagerException;
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
	
	private Mono<DocumentEntity> getErrorIdClientNotFoundException(String documentKey) {
		log.error("getErrorIdClientNotFoundException() : document with documentKey \"{}\" not found", documentKey);
		return Mono.error(new IdClientNotFoundException(documentKey));
	}
    
	@Override
	public Mono<Document> getDocument(String documentKey) {
		log.info("getDocument() : IN : documentKey {}", documentKey);
		
        DynamoDbAsyncTable<DocumentEntity> docTypesTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.DOCUMENT_TABLE_NAME, TableSchema.fromBean(DocumentEntity.class));
        
        return Mono.fromCompletionStage(docTypesTable.getItem(Key.builder().partitionValue(documentKey).build()))
        			.switchIfEmpty(getErrorIdClientNotFoundException(documentKey))
        			.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
        			.map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, Document.class));
	}
	
	@Override
	public Mono<Document> insertDocument(Document documentInput) {
		log.info("insertDocument() : IN : documentInput : {}", documentInput);
		
		if (documentInput == null) {
			throw new RepositoryManagerException("document is null");
		}
		if (documentInput.getDocumentKey()== null) {
			throw new RepositoryManagerException("document Id is null");
		}
		
        DynamoDbAsyncTable<DocumentEntity> documentTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.DOCUMENT_TABLE_NAME, TableSchema.fromBean(DocumentEntity.class));
        DocumentEntity documentEntityInput = objectMapper.convertValue(documentInput, DocumentEntity.class);
        
        return Mono.fromCompletionStage(documentTable.getItem(Key.builder().partitionValue(documentInput.getDocumentKey()).build()))
        	.doOnSuccess( documentFounded -> {
        		if (documentFounded != null) {
        			log.error("insertDocument() : document founded : {}", documentFounded);
        			 throw new ItemAlreadyPresent(documentInput.getDocumentKey());
        		}
        	})
        	.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
        	.doOnSuccess(unused -> documentTable.putItem(builder -> builder.item(documentEntityInput)))
        	.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
        	.map(documentEntity -> objectMapper.convertValue(documentEntity, Document.class));
	}
	
	@Override
	public Mono<Document> patchDocument(String documentKey, Document documentInput) {
		log.info("patchDocument() : IN : documentKey : {} , documentInput {}", documentKey, documentInput);
		
        DynamoDbAsyncTable<DocumentEntity> documentTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.DOCUMENT_TABLE_NAME, TableSchema.fromBean(DocumentEntity.class));
        DocumentEntity documentEntityInput = objectMapper.convertValue(documentInput, DocumentEntity.class);

        return Mono.fromCompletionStage(documentTable.getItem(Key.builder().partitionValue(documentKey).build()))
        		.switchIfEmpty(getErrorIdClientNotFoundException(documentKey))
        		.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
                .doOnSuccess(documentEntityStored -> {
                	log.info("patchDocument() : documentEntityStored : {}", documentEntityStored);
                	// aggiorno solo lo stato
                	if (documentEntityInput.getDocumentState() != null) {
                		documentEntityStored.setDocumentState(documentEntityInput.getDocumentState());
                	}
                	log.info("patchDocument() : documentEntity for patch : {}", documentEntityStored);
                	// Updates an item in the mapped table, or adds it if it doesn't exist. 
                	documentTable.updateItem(documentEntityStored);
                })
                .doOnError(throwable -> log.error(throwable.getMessage(), throwable))
                .map(documentEntity -> objectMapper.convertValue(documentEntity, Document.class));
    	
    }

	@Override
	public Mono<Document> deleteDocument(String documentKey) {
		log.info("deleteDocument() : IN : documentKey {}", documentKey);

        DynamoDbAsyncTable<DocumentEntity> documentTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.DOCUMENT_TABLE_NAME, TableSchema.fromBean(DocumentEntity.class));
        Key typeKey = Key.builder().partitionValue(documentKey).build();
        
        return Mono.fromCompletionStage(documentTable.getItem(typeKey))
        		.switchIfEmpty(getErrorIdClientNotFoundException(documentKey))
        		.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
        		.doOnSuccess(unused -> documentTable.deleteItem(typeKey))
        		.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
        		.map(documentEntity -> objectMapper.convertValue(documentEntity, Document.class));
    }
}

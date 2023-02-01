package it.pagopa.pnss.repositoryManager.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pnss.common.client.enumeration.TipoDocumentoEnum;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.constant.DynamoTableNameConstant;
import it.pagopa.pnss.repositoryManager.entity.DocTypeEntity;
import it.pagopa.pnss.repositoryManager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositoryManager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositoryManager.service.DocTypesService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Service
@Slf4j
public class DocTypesServiceImpl implements DocTypesService {
	
	@Autowired
	private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
	@Autowired
	private DynamoDbEnhancedClient enhancedClient;
	@Autowired
    private ObjectMapper objectMapper;
	
	@Override
	public Mono<DocumentType> getDocType(String typeId) {
	
        DynamoDbAsyncTable<DocTypeEntity> docTypesTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.DOC_TYPES_TABLE_NAME, TableSchema.fromBean(DocTypeEntity.class));
        
        Mono<DocTypeEntity> monoEntity = Mono.fromCompletionStage(docTypesTable.getItem(Key.builder().partitionValue(typeId).build()))
        			.switchIfEmpty(Mono.error(new IdClientNotFoundException(typeId)))
        			.doOnError(IdClientNotFoundException.class, throwable -> log.info(throwable.getMessage()));
        
        return monoEntity.map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, DocumentType.class));
	}
	
	@Override
	public Flux<DocumentType> getAllDocType() {
		
       DynamoDbAsyncTable<DocTypeEntity> docTypesTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.DOC_TYPES_TABLE_NAME, TableSchema.fromBean(DocTypeEntity.class));
       
      return Flux.defer(() -> Flux.from(docTypesTable.scan().items()))               
//							.doOnNext(docTypeEntity -> objectMapper.convertValue(docTypeEntity, DocumentType.class))               
							.doOnError(RepositoryManagerException.class, throwable -> log.info(throwable.getMessage()))
							.map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, DocumentType.class));
		
	}
	
	@Override
	public Mono<DocumentType> insertDocType(DocumentType docTypeInput) {
		
        DynamoDbAsyncTable<DocTypeEntity> docTypesTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.DOC_TYPES_TABLE_NAME, TableSchema.fromBean(DocTypeEntity.class));
        DocTypeEntity docTypeEntityInput = objectMapper.convertValue(docTypeInput, DocTypeEntity.class);
        
        Mono<DocTypeEntity> monoEntity = Mono.fromCompletionStage(docTypesTable.getItem(Key.builder().partitionValue(docTypeInput.getTipoDocumento().getValue()).build()))
        	.handle( (docTypeFounded, sink) -> {
        		if (docTypeFounded != null) {
        			sink.error(new ItemAlreadyPresent(docTypeInput.getTipoDocumento().getValue()));
        		}
        	})
        	.doOnError(ItemAlreadyPresent.class, throwable -> log.info(throwable.getMessage()))
        	.doOnSuccess(unused -> docTypesTable.putItem(builder -> builder.item(docTypeEntityInput)))
        	.thenReturn(docTypeEntityInput);
        
        return monoEntity.map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, DocumentType.class));
        
	}
	
	@Override
	public Mono<DocumentType> updateDocType(String typeId, DocumentType docTypeInput) {
		
        DynamoDbAsyncTable<DocTypeEntity> docTypesTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.DOC_TYPES_TABLE_NAME, TableSchema.fromBean(DocTypeEntity.class));
        DocTypeEntity docTypeEntityInput = objectMapper.convertValue(docTypeInput, DocTypeEntity.class);

        Mono<DocTypeEntity> monoEntity = Mono.fromCompletionStage(docTypesTable.getItem(Key.builder().partitionValue(typeId).build()))
        		.switchIfEmpty(Mono.error(new IdClientNotFoundException(typeId)))
        		.doOnError(IdClientNotFoundException.class, throwable -> log.info(throwable.getMessage()))
                .doOnSuccess(unused -> {
                	docTypeEntityInput.setTipoDocumento(TipoDocumentoEnum.fromValue(typeId));
                	// Puts a single item in the mapped table. 
                	// If the table contains an item with the same primary key, it will be replaced with this item. 
                    docTypesTable.putItem(docTypeEntityInput);
                })
                .thenReturn(docTypeEntityInput);
        
        return monoEntity.map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, DocumentType.class));
	}
	
	@Override
	public Mono<DocumentType> deleteDocType(String typeId) {
		
        DynamoDbAsyncTable<DocTypeEntity> docTypesTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.DOC_TYPES_TABLE_NAME, TableSchema.fromBean(DocTypeEntity.class));
        Key typeKey = Key.builder().partitionValue(typeId).build();
        
        return Mono.fromCompletionStage(docTypesTable.getItem(typeKey))
        		.switchIfEmpty(Mono.error(new IdClientNotFoundException(typeId)))
        		.doOnError(IdClientNotFoundException.class, throwable -> log.info(throwable.getMessage()))
        		.doOnSuccess(unused -> docTypesTable.deleteItem(typeKey))
        		.map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, DocumentType.class));
    }
}

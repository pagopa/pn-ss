package it.pagopa.pnss.repositoryManager.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pnss.common.client.enumeration.TipoDocumentoEnum;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.constant.DynamoTableNameConstant;
import it.pagopa.pnss.repositoryManager.entity.DocTypeEntity;
import it.pagopa.pnss.repositoryManager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositoryManager.service.DocTypesService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Service
@Slf4j
public class DocTypesServiceImpl implements DocTypesService {
	
	@Autowired
	private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
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
	public List<DocumentType> getAllDocType() {
		
//		List<DocumentType> listDocType= new ArrayList<>();
//		
//		try {
//            DynamoDbTable<DocTypeEntity> docTypesTable = enhancedClient.table(
//            		DynamoTableNameConstant.DOC_TYPES_TABLE_NAME, TableSchema.fromBean(DocTypeEntity.class));
//            Iterator<DocTypeEntity> iterator = docTypesTable.scan().items().iterator();
//        	while (iterator.hasNext()) {
//        		listDocType.add(objectMapper.convertValue(iterator.next(), DocumentType.class));
//        	}
//            
//		} catch (DynamoDbException e) {
//			log.error("getAllDocType",e);
//            throw new RepositoryManagerException(e.getMessage());  
//        }
//		
//		return listDocType;
		return null;
		
	}
	
	@Override
	public Mono<DocumentType> insertDocType(DocumentType docTypeInput) {
		
        DynamoDbAsyncTable<DocTypeEntity> docTypesTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.DOC_TYPES_TABLE_NAME, TableSchema.fromBean(DocTypeEntity.class));
        DocTypeEntity docTypeEntityInput = objectMapper.convertValue(docTypeInput, DocTypeEntity.class);
        
        Mono<DocTypeEntity> monoEntity = Mono.fromCompletionStage(docTypesTable.getItem(Key.builder().partitionValue(docTypeInput.getName().getValue()).build()))
        	.handle( (docTypeFounded, sink) -> {
        		if (docTypeFounded != null) {
        			sink.error(new ItemAlreadyPresent(docTypeInput.getName().getValue()));
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
                	docTypeEntityInput.setName(TipoDocumentoEnum.fromValue(typeId));
                    docTypesTable.updateItem(docTypeEntityInput);
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
        		.map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, DocumentType.class))
        		;
    }
}

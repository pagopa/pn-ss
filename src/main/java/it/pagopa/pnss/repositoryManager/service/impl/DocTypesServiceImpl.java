package it.pagopa.pnss.repositoryManager.service.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.TipoDocumentoEnum;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositoryManager.entity.DocTypeEntity;
import it.pagopa.pnss.repositoryManager.exception.DynamoDbException;
import it.pagopa.pnss.repositoryManager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositoryManager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositoryManager.service.DocTypesService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
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
	private RepositoryManagerDynamoTableName repositoryManagerDynamoTableName;
	@Autowired
    private ObjectMapper objectMapper;
	
	private Mono<DocTypeEntity> getErrorIdClientNotFoundException(String typeId) {
		log.error("getErrorIdClientNotFoundException() : docType with typeId \"{}\" not found", typeId);
		return Mono.error(new IdClientNotFoundException(typeId));
	}
		
	@Override
	public Mono<DocumentType> getDocType(String typeId) {
		log.info("getDocType() : IN : typeId {}", typeId);
	
        DynamoDbAsyncTable<DocTypeEntity> docTypesTable = dynamoDbEnhancedAsyncClient.table(
        		repositoryManagerDynamoTableName.tipologieDocumentiName(),
        		//DynamoTableNameConstant.DOC_TYPES_TABLE_NAME,
        		TableSchema.fromBean(DocTypeEntity.class));
        
        return Mono.fromCompletionStage(docTypesTable.getItem(Key.builder().partitionValue(typeId).build()))
        			.switchIfEmpty(getErrorIdClientNotFoundException(typeId))
        			.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
        			.map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, DocumentType.class));
	}
	
//	@Override
//	public Flux<DocumentType> getAllDocType() {
//		
//       DynamoDbAsyncTable<DocTypeEntity> docTypesTable = dynamoDbEnhancedAsyncClient.table(
//        		DynamoTableNameConstant.DOC_TYPES_TABLE_NAME, TableSchema.fromBean(DocTypeEntity.class));
//       
//      return Flux.defer(() -> Flux.from(docTypesTable.scan().items()))               
////							.doOnNext(docTypeEntity -> objectMapper.convertValue(docTypeEntity, DocumentType.class))               
//							.doOnError(RepositoryManagerException.class, throwable -> log.info(throwable.getMessage()))
//							.map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, DocumentType.class));
//		
//	}
	
	@Override
	public List<DocumentType> getAllDocType() {
		log.info("getAllDocType() : START");
		
		List<DocumentType> listDocType= new ArrayList<>();
		
		try {
            DynamoDbTable<DocTypeEntity> docTypesTable = enhancedClient.table(
            		repositoryManagerDynamoTableName.tipologieDocumentiName(),
            		//DynamoTableNameConstant.DOC_TYPES_TABLE_NAME,
            		TableSchema.fromBean(DocTypeEntity.class));
            Iterator<DocTypeEntity> iterator = docTypesTable.scan().items().iterator();
        	while (iterator.hasNext()) {
        		listDocType.add(objectMapper.convertValue(iterator.next(), DocumentType.class));
        	}
            
		} catch (DynamoDbException e) {
			log.error("getAllDocType",e);
            throw new RepositoryManagerException(e.getMessage());  
        }
		
		log.info("getAllDocType() : listDocType {}", listDocType);
		log.info("getAllDocType() : END ");
		return listDocType;
	}
	
	@Override
	public Mono<DocumentType> insertDocType(DocumentType docTypeInput) {
		log.info("insertDocType() : IN : docTypeInput : {}", docTypeInput);
		
		if (docTypeInput == null) {
			throw new RepositoryManagerException("docType is null");
		}
		if (docTypeInput.getTipoDocumento() == null) {
			throw new RepositoryManagerException("docType Id is null");
		}
		
        DynamoDbAsyncTable<DocTypeEntity> docTypesTable = dynamoDbEnhancedAsyncClient.table(
        		repositoryManagerDynamoTableName.tipologieDocumentiName(),
        		//DynamoTableNameConstant.DOC_TYPES_TABLE_NAME,
        		TableSchema.fromBean(DocTypeEntity.class));
        DocTypeEntity docTypeEntityInput = objectMapper.convertValue(docTypeInput, DocTypeEntity.class);
        
        return Mono.fromCompletionStage(docTypesTable.getItem(Key.builder().partitionValue(docTypeInput.getTipoDocumento().getValue()).build()))
        	.doOnSuccess( docTypeFounded -> {
        		log.error("insertDocType() : docType founded : {}", docTypeFounded);
        		if (docTypeFounded != null) {
        			 throw new ItemAlreadyPresent(docTypeInput.getTipoDocumento().getValue());
        		}
        	})
        	.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
        	.doOnSuccess(unused -> docTypesTable.putItem(builder -> builder.item(docTypeEntityInput)))
        	.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
        	.map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, DocumentType.class));
	}
	
	@Override
	public Mono<DocumentType> updateDocType(String typeId, DocumentType docTypeInput) {
		log.info("updateDocType() : IN : typeId : {} , docTypeInput {}", typeId, docTypeInput);
		
        DynamoDbAsyncTable<DocTypeEntity> docTypesTable = dynamoDbEnhancedAsyncClient.table(
        		repositoryManagerDynamoTableName.tipologieDocumentiName(),
        		//DynamoTableNameConstant.DOC_TYPES_TABLE_NAME,
        		TableSchema.fromBean(DocTypeEntity.class));
        DocTypeEntity docTypeEntityInput = objectMapper.convertValue(docTypeInput, DocTypeEntity.class);

       return Mono.fromCompletionStage(docTypesTable.getItem(Key.builder().partitionValue(typeId).build()))
        		.switchIfEmpty(getErrorIdClientNotFoundException(typeId))
        		.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
                .doOnSuccess(unused -> {
                	docTypeEntityInput.setTipoDocumento(TipoDocumentoEnum.fromValue(typeId));
                	// Puts a single item in the mapped table. 
                	// If the table contains an item with the same primary key, it will be replaced with this item. 
                    docTypesTable.putItem(docTypeEntityInput);
                })
                .doOnError(throwable -> log.error(throwable.getMessage(), throwable))
                .map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, DocumentType.class));
	}
	
	@Override
	public Mono<DocumentType> deleteDocType(String typeId) {
		log.info("deleteDocType() : IN : typeId : {}", typeId);
		
        DynamoDbAsyncTable<DocTypeEntity> docTypesTable = dynamoDbEnhancedAsyncClient.table(
        		repositoryManagerDynamoTableName.tipologieDocumentiName(),
        		//DynamoTableNameConstant.DOC_TYPES_TABLE_NAME,
        		TableSchema.fromBean(DocTypeEntity.class));
        Key typeKey = Key.builder().partitionValue(typeId).build();
        
        return Mono.fromCompletionStage(docTypesTable.getItem(typeKey))
        		.switchIfEmpty(getErrorIdClientNotFoundException(typeId))
        		.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
        		.doOnSuccess(unused -> docTypesTable.deleteItem(typeKey))
        		.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
        		.map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, DocumentType.class));
    }
}

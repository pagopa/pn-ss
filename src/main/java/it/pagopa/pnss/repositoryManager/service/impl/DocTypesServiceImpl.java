package it.pagopa.pnss.repositoryManager.service.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pnss.repositoryManager.constant.DynamoTableNameConstant;
import it.pagopa.pnss.repositoryManager.entity.DocTypeEntity;
import it.pagopa.pnss.repositoryManager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositoryManager.exception.ItemDoesNotExist;
import it.pagopa.pnss.repositoryManager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositoryManager.service.DocTypesService;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@Service
@Slf4j
public class DocTypesServiceImpl implements DocTypesService {
	
	@Autowired
	private DynamoDbEnhancedClient enhancedClient;
	@Autowired
    private ObjectMapper objectMapper;
	
	public DocumentType getDocType(String typeId) {
		
		if (typeId.isBlank()) {
			throw new RepositoryManagerException("typeId not specified");
		}
	
		try {
            DynamoDbTable<DocTypeEntity> docTypesTable = enhancedClient.table(
            		DynamoTableNameConstant.DOC_TYPES_TABLE_NAME, TableSchema.fromBean(DocTypeEntity.class));
            
            DocTypeEntity result = docTypesTable.getItem(Key.builder().partitionValue(typeId).build());
            return objectMapper.convertValue(result, DocumentType.class);
            
		} catch (DynamoDbException e) {
            log.error("getDocType",e);
            throw new RepositoryManagerException(e.getMessage());  
        }
	}
	
	public List<DocumentType> getAllDocType() {
		
		List<DocumentType> listDocType= new ArrayList<>();
		
		try {
            DynamoDbTable<DocTypeEntity> docTypesTable = enhancedClient.table(
            		DynamoTableNameConstant.DOC_TYPES_TABLE_NAME, TableSchema.fromBean(DocTypeEntity.class));
            Iterator<DocTypeEntity> iterator = docTypesTable.scan().items().iterator();
        	while (iterator.hasNext()) {
        		listDocType.add(objectMapper.convertValue(iterator.next(), DocumentType.class));
        	}
            
		} catch (DynamoDbException e) {
			log.error("getAllDocType",e);
            throw new RepositoryManagerException(e.getMessage());  
        }
		
		return listDocType;
	}
	
	public DocumentType insertDocType(DocumentType docTypeInput) {
		
		if (docTypeInput == null) {
			throw new RepositoryManagerException("DocType values not specified");
		}
        
        try {
            DynamoDbTable<DocTypeEntity> docTypesTable = enhancedClient.table(
            		DynamoTableNameConstant.DOC_TYPES_TABLE_NAME, TableSchema.fromBean(DocTypeEntity.class));
            DocTypeEntity docTypesEntity = objectMapper.convertValue(docTypeInput, DocTypeEntity.class);
            
            if (docTypesTable.getItem(docTypesEntity) == null) {
            	
            	docTypesTable.putItem(docTypesEntity);
				log.info("DocType data added to the table");
				return objectMapper.convertValue(docTypesEntity, DocumentType.class);

            } else {
            	log.error("DocType cannot be added to the table, DocType id already exists");
            	throw new ItemAlreadyPresent(docTypeInput.getName().getValue());

            }
        } catch (DynamoDbException  e){
            log.error("insertDocTypes",e);
			throw new RepositoryManagerException();
        }
        
	}
	
	public DocumentType updateDocType(String typeId, DocumentType docTypeInput) {
		
		if (typeId == null || typeId.isBlank()) {
			throw new RepositoryManagerException("typeId not specified");
		}
		if (docTypeInput == null) {
			throw new RepositoryManagerException("DocType values not specified");
		}
		if (!docTypeInput.getName().getValue().equals(typeId)) {
			throw new RepositoryManagerException("typeId does not match");
		}
		
		try {
			DynamoDbTable<DocTypeEntity> docTypesTable = enhancedClient.table(
					DynamoTableNameConstant.DOC_TYPES_TABLE_NAME,TableSchema.fromBean(DocTypeEntity.class));
			DocTypeEntity docTypesEntity = objectMapper.convertValue(docTypeInput, DocTypeEntity.class);

			if (docTypesTable.getItem(docTypesEntity) != null) {
				
				docTypesTable.putItem(docTypesEntity);
				log.info("DocType updated");
				return objectMapper.convertValue(docTypesEntity, DocumentType.class);

			} else {
				throw new RepositoryManagerException("DocType cannot be updated: DocType does not exists");
			}
			
		} catch (DynamoDbException e) {
			log.error("updateDocTypes",e);
            throw new RepositoryManagerException();
		}
	}
	
	public void deleteDocType(String typeId) {
		
    	try {
            DynamoDbTable<DocTypeEntity> docTypesTable = enhancedClient.table(
            		DynamoTableNameConstant.DOC_TYPES_TABLE_NAME, TableSchema.fromBean(DocTypeEntity.class));
            DocTypeEntity result = docTypesTable.getItem(Key.builder().partitionValue(typeId).build());
            
            if (result == null) {
            	throw new ItemDoesNotExist(typeId);
            }
            else {
	            docTypesTable.deleteItem(result);
	            log.info("DocType deleted: name {}",typeId);
            }
                          
    	} catch (DynamoDbException  e) {
            log.error("deleteDocTypes",e);
            throw new RepositoryManagerException();
        }
    }
}

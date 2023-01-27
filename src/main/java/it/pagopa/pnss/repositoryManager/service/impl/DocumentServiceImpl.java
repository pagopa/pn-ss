package it.pagopa.pnss.repositoryManager.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pnss.repositoryManager.constant.DynamoTableNameConstant;
import it.pagopa.pnss.repositoryManager.entity.DocumentEntity;
import it.pagopa.pnss.repositoryManager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositoryManager.exception.ItemDoesNotExist;
import it.pagopa.pnss.repositoryManager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositoryManager.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@Service
@Slf4j
public class DocumentServiceImpl implements DocumentService {
	
	@Autowired
	private DynamoDbEnhancedClient enhancedClient;
	@Autowired
    private ObjectMapper objectMapper;
    
	public Document getDocument(String documentKey) {
		
		try {
            DynamoDbTable<DocumentEntity> documentTable = enhancedClient.table(
            		DynamoTableNameConstant.DOCUMENT_TABLE_NAME, TableSchema.fromBean(DocumentEntity.class));
            DocumentEntity result = documentTable.getItem(Key.builder().partitionValue(documentKey).build());
            return objectMapper.convertValue(result, Document.class);
            
		} catch (DynamoDbException e) {
            log.error("getDocument",e);
            throw new RepositoryManagerException();  
        }
	}
	
	public Document insertDocument(Document documentInput) {
		
		if (documentInput == null) {
			throw new RepositoryManagerException("Document values not specified");
		}
   
        try {
            DynamoDbTable<DocumentEntity> documentTable = enhancedClient.table(DynamoTableNameConstant.DOCUMENT_TABLE_NAME, TableSchema.fromBean(DocumentEntity.class));
            DocumentEntity documentEntity = objectMapper.convertValue(documentInput, DocumentEntity.class);
                       
            if (documentTable.getItem(documentEntity) == null) {
            	
            	documentTable.putItem(documentEntity);
				log.info("Document added to the table");
				return objectMapper.convertValue(documentEntity, Document.class);
				
            } else {
            	throw new ItemAlreadyPresent(documentInput.getDocumentKey());
            }
            
		} catch (DynamoDbException e) {
			log.error(e.getMessage());
			throw new RepositoryManagerException();
		}

	}
	
	public Document patchDocument(String documentKey, Document documentInput) {
		
		if (documentKey == null || documentKey.isBlank()) {
			throw new RepositoryManagerException("Document key not specified");
		}
		if (documentInput == null) {
			throw new RepositoryManagerException("Document values not specified");
		}
		if (!documentInput.getDocumentKey().isBlank() && !documentInput.getDocumentKey().equals(documentKey)) {
			throw new RepositoryManagerException("Document key does not match");
		}
    	
    	try {
            DynamoDbTable<DocumentEntity> documentTable = enhancedClient.table(DynamoTableNameConstant.DOCUMENT_TABLE_NAME, TableSchema.fromBean(DocumentEntity.class));
            DocumentEntity documentEntity = documentTable.getItem(Key.builder().partitionValue(documentKey).build());

            if (documentTable.getItem(documentEntity) != null) { 
            	
            	if (documentInput.getDocumentState() != null) {
            		documentEntity.setDocumentState(documentInput.getDocumentState());
            	}
            	if (documentInput.getRetentionPeriod() != null && !documentInput.getRetentionPeriod().isBlank()) {
            		documentEntity.setRetentionPeriod(documentInput.getRetentionPeriod());
            	}
            	if (documentInput.getContentLenght() != null && !documentInput.getContentLenght().isBlank()) {
            		documentEntity.setContentLenght(documentInput.getContentLenght());
            	}
            	
	            documentTable.updateItem(documentEntity);
	            log.info("Document updated");
	            return objectMapper.convertValue(documentEntity, Document.class);
	            
	    	} else {
	    		throw new RepositoryManagerException("Document cannot be updated: Document does not exists");
    	    }
    		
    	} catch (DynamoDbException  e){
            System.err.println(e.getMessage());
            throw new RepositoryManagerException();
        }    	
    	
    }

	public void deleteDocument(String documentKey) {

    	try {
            DynamoDbTable<DocumentEntity> documentTable = enhancedClient.table(DynamoTableNameConstant.DOCUMENT_TABLE_NAME, TableSchema.fromBean(DocumentEntity.class));
            DocumentEntity result = documentTable.getItem(Key.builder().partitionValue(documentKey).build());
            
            if (result == null) {
            	throw new ItemDoesNotExist(documentKey);
            }
            else {
            	documentTable.deleteItem(result);
	            log.info("Document deleted");   
            }
            
    	} catch (DynamoDbException  e) {
            log.error("deleteDocument",e.getMessage());
            throw new RepositoryManagerException();
        }    	
    }
}

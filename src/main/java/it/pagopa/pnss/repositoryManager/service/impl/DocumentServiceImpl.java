package it.pagopa.pnss.repositoryManager.service.impl;

import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pnss.repositoryManager.constant.DynamoTableNameConstant;
import it.pagopa.pnss.repositoryManager.dto.DocumentInput;
import it.pagopa.pnss.repositoryManager.dto.DocumentOutput;
import it.pagopa.pnss.repositoryManager.entity.DocumentEntity;
import it.pagopa.pnss.repositoryManager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositoryManager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositoryManager.service.DocumentService;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@Service
public class DocumentServiceImpl implements DocumentService {
	
	@Autowired
	private DynamoDbEnhancedClient enhancedClient;
	@Autowired
    private ObjectMapper objectMapper;
    
	public DocumentOutput getDocument(String documentKey) {
		try {
            DynamoDbTable<DocumentEntity> documentTable = enhancedClient.table(DynamoTableNameConstant.DOCUMENT_TABLE_NAME, TableSchema.fromBean(DocumentEntity.class));
            QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(documentKey).build());
            
            Iterator<DocumentEntity> result = documentTable.query(queryConditional).items().iterator();
		
            DocumentEntity docType = result.next();
            return objectMapper.convertValue(docType, DocumentOutput.class);
            
		} catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            throw new RepositoryManagerException();  
        }
	}
	
	public DocumentOutput postdocument(DocumentInput documentInput) {
   
        try {
            DynamoDbTable<DocumentEntity> documentTable = enhancedClient.table(DynamoTableNameConstant.DOCUMENT_TABLE_NAME, TableSchema.fromBean(DocumentEntity.class));
            DocumentEntity documentEntity = objectMapper.convertValue(documentInput, DocumentEntity.class);
                       
            if(documentTable.getItem(documentEntity) == null) {
            	documentTable.putItem(documentEntity);
				System.out.println("User data added to the table");
				
				return objectMapper.convertValue(documentEntity, DocumentOutput.class);
            } else {
            	throw new ItemAlreadyPresent(documentInput.getCheckSum());
            }
		} catch (DynamoDbException e) {
			System.err.println(e.getMessage());
			throw new RepositoryManagerException();
		}

	}
	
	public DocumentOutput updatedocument(DocumentInput documentInput) {
    	
    	try {
            DynamoDbTable<DocumentEntity> documentTable = enhancedClient.table(DynamoTableNameConstant.DOCUMENT_TABLE_NAME, TableSchema.fromBean(DocumentEntity.class));
            DocumentEntity documentEntity = objectMapper.convertValue(documentInput, DocumentEntity.class);

            if(documentTable.getItem(documentEntity) != null) { 
	            documentTable.putItem(documentEntity);
	            System.out.println("Modifica avvenuta con successo");
	            return objectMapper.convertValue(documentEntity, DocumentOutput.class);
	    	} else {
	    		throw new RepositoryManagerException();
    	    }
    		
    	} catch (DynamoDbException  e){
            System.err.println(e.getMessage());
            throw new RepositoryManagerException();
        }    	
    	
    }

	public DocumentOutput deletedocument(String documentKey) {

    	try {
            DynamoDbTable<DocumentEntity> documentTable = enhancedClient.table(DynamoTableNameConstant.DOCUMENT_TABLE_NAME, TableSchema.fromBean(DocumentEntity.class));
            QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(documentKey).build());
            Iterator<DocumentEntity> result = documentTable.query(queryConditional).items().iterator();
            
            DocumentEntity documentEntity = result.next();                        
            documentTable.deleteItem(documentEntity);    
            System.out.println("Cancellazione avvenuta con successo");
            return objectMapper.convertValue(documentEntity, DocumentOutput.class);              
            
    	}catch (DynamoDbException  e){
            System.err.println(e.getMessage());
            throw new RepositoryManagerException();
        }    	
    }
}

package it.pagopa.pnss.repositoryManager.service;

import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pnss.repositoryManager.dto.DocumentInput;
import it.pagopa.pnss.repositoryManager.dto.DocumentOutput;
import it.pagopa.pnss.repositoryManager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositoryManager.model.DocumentEntity;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@Service
public class DocumentService {
	
//	private final DynamoDbEnhancedClient enhancedClient;
//    private final ObjectMapper objectMapper;
//    
//    public DocumentService(DynamoDbEnhancedClient enhancedClient, ObjectMapper objectMapper) {
//        this.enhancedClient = enhancedClient;
//        this.objectMapper = objectMapper;
//    }
	
	@Autowired
	private DynamoDbEnhancedClient enhancedClient;
	@Autowired
	private ObjectMapper objectMapper;

	public DocumentOutput getDocument(String name) {
		try {
            DynamoDbTable<DocumentEntity> documentTable = enhancedClient.table("Document", TableSchema.fromBean(DocumentEntity.class));
            QueryConditional queryConditional = QueryConditional
                    .keyEqualTo(Key.builder()
                            .partitionValue(name)
                            .build());
            
            Iterator<DocumentEntity> result = documentTable.query(queryConditional).items().iterator();
		
            DocumentEntity docType = result.next();
            return objectMapper.convertValue(docType, DocumentOutput.class);
            
            
		} catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            throw new RepositoryManagerException.DynamoDbException();  
        }
	}
	
	public DocumentOutput postdocument(DocumentInput documentInput) {
   
        try {
            DynamoDbTable<DocumentEntity> documentTable = enhancedClient.table("Document",
            		                                                           TableSchema.fromBean(
            		                                                        		   DocumentEntity.class));
            DocumentEntity documentEntity = objectMapper.convertValue(documentInput, DocumentEntity.class);
                       
            if(documentTable.getItem(documentEntity) == null) {
            	documentTable.putItem(documentEntity);
				System.out.println("User data added to the table");
				
				return objectMapper.convertValue(documentEntity, DocumentOutput.class);
            } else {
            	throw new RepositoryManagerException.IdClientAlreadyPresent(documentInput.getDocumentKey());
            }
		} catch (DynamoDbException e) {
			System.err.println(e.getMessage());
			throw new RepositoryManagerException.DynamoDbException();
		}

	}
	
	public DocumentOutput updatedocument(DocumentInput documentInput) {
    	
    	try {
            DynamoDbTable<DocumentEntity> documentTable = enhancedClient.table("Document", TableSchema.fromBean(DocumentEntity.class));
            DocumentEntity documentEntity = objectMapper.convertValue(documentInput, DocumentEntity.class);

            if(documentTable.getItem(documentEntity) != null) { 
            documentTable.putItem(documentEntity);
            return objectMapper.convertValue(documentEntity, DocumentOutput.class);
                      
    	} else {
    		throw new RepositoryManagerException.DynamoDbException();
    	    }
    		
    	} catch (DynamoDbException  e){
            System.err.println(e.getMessage());
            throw new RepositoryManagerException.DynamoDbException();
        }    	
    	
    }

	public DocumentOutput deletedocument(String name) {

    	try {
            DynamoDbTable<DocumentEntity> documentTable = enhancedClient.table("Document", TableSchema.fromBean(DocumentEntity.class));
            QueryConditional queryConditional = QueryConditional
                    .keyEqualTo(Key.builder()
                            .partitionValue(name)
                            .build());
            Iterator<DocumentEntity> result = documentTable.query(queryConditional).items().iterator();
            
            DocumentEntity documentEntity = result.next();                        
            documentTable.deleteItem(documentEntity);    
            System.out.println("Cancellazione avvenuta con successo");
            return objectMapper.convertValue(documentEntity, DocumentOutput.class);              
            
    	}catch (DynamoDbException  e){
            System.err.println(e.getMessage());
            throw new RepositoryManagerException.DynamoDbException();
        }    	
    }
}

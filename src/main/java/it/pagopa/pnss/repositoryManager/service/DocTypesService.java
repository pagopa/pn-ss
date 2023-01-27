package it.pagopa.pnss.repositoryManager.service;

import java.util.Iterator;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pnss.repositoryManager.constant.DynamoTableNameConstant;
import it.pagopa.pnss.repositoryManager.dto.DocTypesInput;
import it.pagopa.pnss.repositoryManager.dto.DocTypesOutput;
import it.pagopa.pnss.repositoryManager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositoryManager.model.DocTypesEntity;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@Service
public class DocTypesService {
	
	private final DynamoDbEnhancedClient enhancedClient;
    private final ObjectMapper objectMapper;
    
    public DocTypesService(DynamoDbEnhancedClient enhancedClient, ObjectMapper objectMapper) {
        this.enhancedClient = enhancedClient;
        this.objectMapper = objectMapper;
    }
	
	public DocTypesOutput getDocType(String checkSum) {
	
		try {
            DynamoDbTable<DocTypesEntity> docTypesTable = enhancedClient.table(DynamoTableNameConstant.DOC_TYPES_TABLE_NAME, TableSchema.fromBean(DocTypesEntity.class));
            QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(checkSum).build());
            
            Iterator<DocTypesEntity> result = docTypesTable.query(queryConditional).items().iterator();
		
            DocTypesEntity docType = result.next();
            
            return objectMapper.convertValue(docType, DocTypesOutput.class);
            
		} catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            throw new RepositoryManagerException.DynamoDbException();  
        }
	}
	
	public DocTypesOutput postDocTypes(DocTypesInput docTypesInput) {
        
        try {
            DynamoDbTable<DocTypesEntity> docTypesTable = enhancedClient.table(DynamoTableNameConstant.DOC_TYPES_TABLE_NAME, TableSchema.fromBean(DocTypesEntity.class));
            
            DocTypesEntity docTypesEntity = objectMapper.convertValue(docTypesInput, DocTypesEntity.class);
            
            
            if(docTypesTable.getItem(docTypesEntity) == null) {
            	
            	docTypesTable.putItem(docTypesEntity);
				System.out.println("DocType data added to the table");
	            
				return objectMapper.convertValue(docTypesEntity, DocTypesOutput.class);

            } else {
				System.out.println("User cannot be added to the table, user id already exists");
            	throw new RepositoryManagerException.IdClientAlreadyPresent(docTypesInput.getCheckSum().name());

            }
        } catch (DynamoDbException  e){
            System.err.println(e.getMessage());
			throw new RepositoryManagerException.DynamoDbException();

        }
        
	}
	
	public DocTypesOutput updateDocTypes(DocTypesInput docTypesInput) {
		
		try {
			DynamoDbTable<DocTypesEntity> docTypesTable = enhancedClient.table(DynamoTableNameConstant.DOC_TYPES_TABLE_NAME,
					TableSchema.fromBean(DocTypesEntity.class));
			DocTypesEntity docTypesEntity = objectMapper.convertValue(docTypesInput, DocTypesEntity.class);

			if (docTypesTable.getItem(docTypesEntity) != null) {
				docTypesTable.putItem(docTypesEntity);
				System.out.println("Modifica avvenuta con successo");
				return objectMapper.convertValue(docTypesEntity, DocTypesOutput.class);

			} else {
				throw new RepositoryManagerException.DynamoDbException();
			}
		} catch (DynamoDbException e) {
			System.err.println(e.getMessage());
            throw new RepositoryManagerException.DynamoDbException();
		}
	}
	
	public DocTypesOutput deleteDocTypes(String checkSum) {
		
    	try {
            DynamoDbTable<DocTypesEntity> docTypesTable = enhancedClient.table(DynamoTableNameConstant.DOC_TYPES_TABLE_NAME, TableSchema.fromBean(DocTypesEntity.class));
            QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(checkSum).build());
            Iterator<DocTypesEntity> result = docTypesTable.query(queryConditional).items().iterator();
            
            DocTypesEntity docTypesEntity = result.next();
            docTypesTable.deleteItem(docTypesEntity);
            
            System.out.println("Cancellazione avvenuta con successo");
            return objectMapper.convertValue(docTypesEntity, DocTypesOutput.class);               
    	}catch (DynamoDbException  e){
            System.err.println(e.getMessage());
            throw new RepositoryManagerException.DynamoDbException();
        }
    }
}

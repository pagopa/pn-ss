package it.pagopa.pnss.repositoryManager.service;

import java.util.Iterator;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pnss.repositoryManager.DependencyFactory;
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

public class DocTypesService {
	
	private final DynamoDbEnhancedClient enhancedClient;
    private final ObjectMapper objectMapper;
    
    public DocTypesService(DynamoDbEnhancedClient enhancedClient, ObjectMapper objectMapper) {
        this.enhancedClient = enhancedClient;
        this.objectMapper = objectMapper;
    }
	
	public DocTypesOutput getDocType(String partition_id) {
		DocTypesOutput docTypesOutput = new DocTypesOutput();
		try {
            DynamoDbTable<DocTypesEntity> DocTypesTable = enhancedClient.table("DocTpesEntity", TableSchema.fromBean(DocTypesEntity.class));
            QueryConditional queryConditional = QueryConditional
                    .keyEqualTo(Key.builder()
                            .partitionValue(partition_id)
                            .build());
            
            Iterator<DocTypesEntity> result = DocTypesTable.query(queryConditional).items().iterator();
		
            DocTypesEntity docType = result.next();
            docTypesOutput = objectMapper.convertValue(docType, DocTypesOutput.class);
            
            return docTypesOutput;
		} catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            throw new RepositoryManagerException.DynamoDbException();  
        }
	}
	
	public DocTypesOutput postDocTypes(DocTypesInput docTypesInput) {
        
        try {
            DynamoDbTable<DocTypesEntity> DocTypesTable = enhancedClient.table("DocTypes", TableSchema.fromBean(DocTypesEntity.class));
            DocTypesEntity docTypesEntity = objectMapper.convertValue(docTypesInput, DocTypesEntity.class);
            
            
            if(DocTypesTable.getItem(docTypesEntity) == null) {
            	DocTypesTable.putItem(docTypesEntity);
				System.out.println("DocType data added to the table");
	            
				return objectMapper.convertValue(docTypesEntity, DocTypesOutput.class);

            } else {
				System.out.println("User cannot be added to the table, user id already exists");
            	throw new RepositoryManagerException.IdClientAlreadyPresent(docTypesInput.getName());

            }
        } catch (DynamoDbException  e){
            System.err.println(e.getMessage());
			throw new RepositoryManagerException.DynamoDbException();

        }
        
	}
	
	public DocTypesOutput updateDocTypes(DocTypesInput docTypesInput) {
		DocTypesOutput docTypesOutput = new DocTypesOutput();
		try {
			DynamoDbTable<DocTypesEntity> docTypesTable = enhancedClient.table("DocTypes",
					TableSchema.fromBean(DocTypesEntity.class));
			DocTypesEntity docTypesEntity = objectMapper.convertValue(docTypesInput, DocTypesEntity.class);

			if (docTypesTable.getItem(docTypesEntity) != null) {
				docTypesTable.putItem(docTypesEntity);
				docTypesOutput = objectMapper.convertValue(docTypesEntity, DocTypesOutput.class);

			} else {
				throw new RepositoryManagerException.DynamoDbException();
			}
		} catch (DynamoDbException e) {
			System.err.println(e.getMessage());
            throw new RepositoryManagerException.DynamoDbException();
		}

		return docTypesOutput;
	}
	
	public DocTypesOutput deleteDocTypes(String name) {
    	DynamoDbEnhancedClient enhancedClient = DependencyFactory.dynamoDbEnhancedClient();
        DocTypesOutput docTypesResponse = new DocTypesOutput();
    	try {
            DynamoDbTable<DocTypesEntity> docTypesTable = enhancedClient.table("DocTypes", TableSchema.fromBean(DocTypesEntity.class));
            QueryConditional queryConditional = QueryConditional
                    .keyEqualTo(Key.builder()
                            .partitionValue(name)
                            .build());
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

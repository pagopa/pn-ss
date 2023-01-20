package it.pagopa.pnss.repositoryManager.service;

import java.util.Iterator;

import org.springframework.beans.BeanUtils;

import it.pagopa.pnss.repositoryManager.DependencyFactory;
import it.pagopa.pnss.repositoryManager.dto.DocTypesInput;
import it.pagopa.pnss.repositoryManager.dto.DocTypesOutput;
import it.pagopa.pnss.repositoryManager.model.DocTypesEntity;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

public class DocTypesService {
	
	public DocTypesOutput getDocType(String partition_id) {
		DocTypesOutput docTypesResponse = new DocTypesOutput();
		try {
            DynamoDbEnhancedClient enhancedClient = DependencyFactory.dynamoDbEnhancedClient();

            DynamoDbTable<DocTypesEntity> DocTypesTable = enhancedClient.table("DocTpesEntity", TableSchema.fromBean(DocTypesEntity.class));
            QueryConditional queryConditional = QueryConditional
                    .keyEqualTo(Key.builder()
                            .partitionValue(partition_id)
                            .build());
            
            Iterator<DocTypesEntity> result = DocTypesTable.query(queryConditional).items().iterator();
		
            DocTypesEntity docType = result.next();
            BeanUtils.copyProperties(docType, docTypesResponse);
            
            return docTypesResponse;
		} catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return null;
        }
	}
	
	public DocTypesOutput postDocTypes(DocTypesInput docTypesInput) {
        DynamoDbEnhancedClient enhancedClient = DependencyFactory.dynamoDbEnhancedClient();
        DocTypesOutput docTypesResponse = new DocTypesOutput();
        try {
            DynamoDbTable<DocTypesEntity> DocTypesTable = enhancedClient.table("DocTypes", TableSchema.fromBean(DocTypesEntity.class));
            DocTypesEntity docTypesEntity = new DocTypesEntity();
            BeanUtils.copyProperties(docTypesInput, docTypesEntity);
            
            if(DocTypesTable.getItem(docTypesEntity) == null) {
            	DocTypesTable.putItem(docTypesEntity);
				System.out.println("User data added to the table");
	            BeanUtils.copyProperties(docTypesInput, docTypesResponse);

            } else {
				System.out.println("User cannot be added to the table, user id already exists");
            }
        } catch (DynamoDbException  e){
            System.err.println(e.getMessage());
            //System.exit(1);
        }
        return docTypesResponse;
	}
	
	public DocTypesOutput updateDocTypes(DocTypesInput docTypesInput) {
    	DynamoDbEnhancedClient enhancedClient = DependencyFactory.dynamoDbEnhancedClient();
        DocTypesOutput docTypesResponse = new DocTypesOutput();
    	try {
            DynamoDbTable<DocTypesEntity> DocTypesTable = enhancedClient.table("DocTypes", TableSchema.fromBean(DocTypesEntity.class));
            DocTypesEntity docTypesEntity = new DocTypesEntity();
            BeanUtils.copyProperties(docTypesInput, docTypesEntity);
            DocTypesTable.putItem(docTypesEntity);
            BeanUtils.copyProperties(docTypesInput, docTypesResponse);
            
    	}catch (DynamoDbException  e){
            System.err.println(e.getMessage());
            //System.exit(1);
        }
    	
    	return docTypesResponse;
    }
	
	public DocTypesOutput deleteDocTypes(DocTypesInput docTypesInput) {
    	DynamoDbEnhancedClient enhancedClient = DependencyFactory.dynamoDbEnhancedClient();
        DocTypesOutput docTypesResponse = new DocTypesOutput();
    	try {
            DynamoDbTable<DocTypesEntity> DocTypesTable = enhancedClient.table("DocTypes", TableSchema.fromBean(DocTypesEntity.class));
            DocTypesEntity docTypesEntity = new DocTypesEntity();
            BeanUtils.copyProperties(docTypesInput, docTypesEntity);
            DocTypesTable.deleteItem(docTypesEntity);
            BeanUtils.copyProperties(docTypesInput, docTypesResponse);
            
            
    	}catch (DynamoDbException  e){
            System.err.println(e.getMessage());
            //System.exit(1);
        }
    	
    	return docTypesResponse;
    }

}

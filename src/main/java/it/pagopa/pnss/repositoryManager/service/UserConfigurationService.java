package it.pagopa.pnss.repositoryManager.service;


import java.util.Iterator;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pnss.repositoryManager.dto.UserConfigurationInput;
import it.pagopa.pnss.repositoryManager.dto.UserConfigurationOutput;
import it.pagopa.pnss.repositoryManager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositoryManager.model.UserConfigurationEntity;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;




public class UserConfigurationService {
	
	private final DynamoDbEnhancedClient enhancedClient;
    private final ObjectMapper objectMapper;
    
    public UserConfigurationService(DynamoDbEnhancedClient enhancedClient, ObjectMapper objectMapper) {
        this.enhancedClient = enhancedClient;
        this.objectMapper = objectMapper;
    }


    public UserConfigurationOutput getUser(String name) {
        try {
            DynamoDbTable<UserConfigurationEntity> userConfigurationTable = enhancedClient.table("UserConfiguration", TableSchema.fromBean(UserConfigurationEntity.class));
            QueryConditional queryConditional = QueryConditional
                    .keyEqualTo(Key.builder()
                            .partitionValue(name)
                            .build());

            Iterator<UserConfigurationEntity> result = userConfigurationTable.query(queryConditional).items().iterator();

             UserConfigurationEntity user = result.next();
             
             return objectMapper.convertValue(user, UserConfigurationOutput.class);

        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            throw new RepositoryManagerException.DynamoDbException();  
        }
    }
    
    public UserConfigurationOutput postUser(UserConfigurationInput userInput){

    	try {
            DynamoDbTable<UserConfigurationEntity> userConfigurationTable = enhancedClient.table("UserConfiguration", TableSchema.fromBean(UserConfigurationEntity.class));

            UserConfigurationEntity userEntity = objectMapper.convertValue(userInput, UserConfigurationEntity.class);
            
			if (userConfigurationTable.getItem(userEntity) == null) {

				userConfigurationTable.putItem(userEntity);
				System.out.println("User inserito nel db ");

				return objectMapper.convertValue(userEntity, UserConfigurationOutput.class); 
				
			} else {
				System.out.println("L'utente non può essere aggiunto, id già esistente");
            	throw new RepositoryManagerException.IdClientAlreadyPresent(userInput.getName());

			}
        }catch (DynamoDbException  e){
            System.err.println(e.getMessage());
			throw new RepositoryManagerException.DynamoDbException();

        }
        
        
    }
    
    public UserConfigurationOutput updateUser(UserConfigurationInput user) {
    	
    	try {
            DynamoDbTable<UserConfigurationEntity> userConfigurationTable = enhancedClient.table("UserConfiguration", TableSchema.fromBean(UserConfigurationEntity.class));
            UserConfigurationEntity userEntity = objectMapper.convertValue(user, UserConfigurationEntity.class);
            
            if (userConfigurationTable.getItem(userEntity) != null) {
            userConfigurationTable.putItem(userEntity);
            System.out.println("Modifica avvenuta con successo");
            return objectMapper.convertValue(userEntity, UserConfigurationOutput.class);
            } else {
        		throw new RepositoryManagerException.DynamoDbException();
            }            
    	}catch (DynamoDbException  e){
            System.err.println(e.getMessage());
    		throw new RepositoryManagerException.DynamoDbException();
        }
    }
    
    public UserConfigurationOutput deleteUser(String name) {
    	
    	try {
            DynamoDbTable<UserConfigurationEntity> userConfigurationTable = enhancedClient.table("UserConfiguration", TableSchema.fromBean(UserConfigurationEntity.class));
            QueryConditional queryConditional = QueryConditional
                    .keyEqualTo(Key.builder()
                            .partitionValue(name)
                            .build());
            Iterator<UserConfigurationEntity> result = userConfigurationTable.query(queryConditional).items().iterator();
            UserConfigurationEntity userEntity = result.next();
            userConfigurationTable.deleteItem(userEntity);
            System.out.println("Cancellazione avvenuta con successo");
            return objectMapper.convertValue(userEntity, UserConfigurationOutput.class);              

            
    	}catch (DynamoDbException  e){
            System.err.println(e.getMessage());
            throw new RepositoryManagerException.DynamoDbException();

        }
    }
    
    
}

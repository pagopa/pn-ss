package it.pagopa.pnss.repositoryManager.service;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.BeanUtils;

import it.pagopa.pnss.repositoryManager.DependencyFactory;
import it.pagopa.pnss.repositoryManager.dto.UserConfigurationOutput;
import it.pagopa.pnss.repositoryManager.dto.UserConfigurationInput;
import it.pagopa.pnss.repositoryManager.model.UserConfigurationEntity;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;




public class UserConfigurationService {


    public UserConfigurationOutput getUser(String name) {
    	UserConfigurationOutput userResponse = new UserConfigurationOutput();
        try {
            DynamoDbEnhancedClient enhancedClient = DependencyFactory.dynamoDbEnhancedClient();

            DynamoDbTable<UserConfigurationEntity> UserConfigurationTable = enhancedClient.table("UserConfiguration", TableSchema.fromBean(UserConfigurationEntity.class));
            QueryConditional queryConditional = QueryConditional
                    .keyEqualTo(Key.builder()
                            .partitionValue(name)
                            .build());

            Iterator<UserConfigurationEntity> result = UserConfigurationTable.query(queryConditional).items().iterator();

             UserConfigurationEntity user = result.next();
             BeanUtils.copyProperties(user, userResponse);
             
            return userResponse;

        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return null;
        }
    }
    
    public UserConfigurationOutput postUser(UserConfigurationInput userInput){
        DynamoDbEnhancedClient enhancedClient = DependencyFactory.dynamoDbEnhancedClient();
        UserConfigurationOutput userResponse = new UserConfigurationOutput();
        try {
            DynamoDbTable<UserConfigurationEntity> UserConfigurationTable = enhancedClient.table("UserConfiguration", TableSchema.fromBean(UserConfigurationEntity.class));

            UserConfigurationEntity userEntity = new UserConfigurationEntity();
            
            BeanUtils.copyProperties(userInput, userEntity);
            
			if (UserConfigurationTable.getItem(userEntity) == null) {

				UserConfigurationTable.putItem(userEntity);
				System.out.println("User inserito nel db ");

				BeanUtils.copyProperties(userEntity, userResponse);
				
			} else {
				System.out.println("L'utente non può essere aggiunto, id già esistente");
			}
        }catch (DynamoDbException  e){
            System.err.println(e.getMessage());
            //System.exit(1);
        }
        
        return userResponse;
    }
    
    public UserConfigurationOutput updateUser(UserConfigurationInput user) {
    	DynamoDbEnhancedClient enhancedClient = DependencyFactory.dynamoDbEnhancedClient();
    	UserConfigurationOutput userResponse = new UserConfigurationOutput();
    	try {
            DynamoDbTable<UserConfigurationEntity> UserConfigurationTable = enhancedClient.table("UserConfiguration", TableSchema.fromBean(UserConfigurationEntity.class));
            UserConfigurationEntity userEntity = new UserConfigurationEntity();
            
            BeanUtils.copyProperties(user, userEntity);
            UserConfigurationTable.putItem(userEntity);
            BeanUtils.copyProperties(user, userResponse);
            
            System.out.println("Modifica avvenuta con successo");
    	}catch (DynamoDbException  e){
            System.err.println(e.getMessage());
            //System.exit(1);
        }
    	
    	return userResponse;
    }
    
    public UserConfigurationOutput deleteUser(UserConfigurationInput user) {
    	DynamoDbEnhancedClient enhancedClient = DependencyFactory.dynamoDbEnhancedClient();
    	UserConfigurationOutput userResponse = new UserConfigurationOutput();
    	try {
            DynamoDbTable<UserConfigurationEntity> UserConfigurationTable = enhancedClient.table("UserConfiguration", TableSchema.fromBean(UserConfigurationEntity.class));
            UserConfigurationEntity userEntity = new UserConfigurationEntity();
            
            BeanUtils.copyProperties(user, userEntity);
            UserConfigurationTable.deleteItem(userEntity);
            BeanUtils.copyProperties(user, userResponse);
          
            
            System.out.println("Cancellazione avvenuta con successo");
            
    	}catch (DynamoDbException  e){
            System.err.println(e.getMessage());
            //System.exit(1);
        }
    	
    	return userResponse;
    }
    
    
}

package it.pagopa.pnss.repositorymanager.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.internal.rest.v1.dto.UserConfiguration;
import it.pagopa.pnss.repositorymanager.constant.DynamoTableNameConstant;
import it.pagopa.pnss.repositorymanager.entity.UserConfigurationEntity;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.ItemDoesNotExist;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.UserConfigurationService;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@Service
@Slf4j
public class UserConfigurationServiceImpl implements UserConfigurationService {

	@Autowired
	private DynamoDbEnhancedClient enhancedClient;
	@Autowired
	private ObjectMapper objectMapper;

	public UserConfiguration getUserConfiguration(String name) {
		
		try {
			DynamoDbTable<UserConfigurationEntity> userConfigurationTable = enhancedClient.table(
					DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME,TableSchema.fromBean(UserConfigurationEntity.class));
			UserConfigurationEntity result = userConfigurationTable.getItem(Key.builder().partitionValue(name).build());
            return objectMapper.convertValue(result, UserConfiguration.class);

		} catch (DynamoDbException e) {
			log.error("getUser", e);
			throw new RepositoryManagerException();
		}
	}

	public UserConfiguration insertUserConfiguration(UserConfiguration userConfiguration) {
		
		if (userConfiguration == null) {
			throw new RepositoryManagerException("Document values not specified");
		}

		try {
			DynamoDbTable<UserConfigurationEntity> userConfigurationTable = enhancedClient.table(
					DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME,TableSchema.fromBean(UserConfigurationEntity.class));
			UserConfigurationEntity userEntity = objectMapper.convertValue(userConfiguration,UserConfigurationEntity.class);

			if (userConfigurationTable.getItem(userEntity) == null) {

				userConfigurationTable.putItem(userEntity);
				log.info("User Configuration added to the table");
				return objectMapper.convertValue(userEntity, UserConfiguration.class);

			} else {
				throw new ItemAlreadyPresent(userConfiguration.getName());
			}
			
		} catch (DynamoDbException e) {
			log.error("insertUserConfiguration",e);
			throw new RepositoryManagerException();
		}

	}

	public UserConfiguration patchUserConfiguration(String name, UserConfiguration userConfigurationInput) {
		
		if (name == null || name.isBlank()) {
			throw new RepositoryManagerException("User configuration name not specified");
		}
		if (userConfigurationInput == null) {
			throw new RepositoryManagerException("User configuration values not specified");
		}
		if (!userConfigurationInput.getName().isBlank() && !userConfigurationInput.getName().equals(name)) {
			throw new RepositoryManagerException("User configuration key does not match");
		}

		try {
			DynamoDbTable<UserConfigurationEntity> userConfigurationTable = enhancedClient.table(
					DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME,TableSchema.fromBean(UserConfigurationEntity.class));
			UserConfigurationEntity userEntity = userConfigurationTable.getItem(Key.builder().partitionValue(name).build());

            if (userConfigurationTable.getItem(userEntity) != null) { 
            	
            	userEntity.setCanRead(userConfigurationInput.getCanRead());
            	userEntity.setCanCreate(userConfigurationInput.getCanCreate());
            	
            	userConfigurationTable.updateItem(userEntity);
	            log.info("User Configuration updated");
	            return objectMapper.convertValue(userEntity, UserConfiguration.class);
	            
	    	} else {
	    		throw new RepositoryManagerException("User Configuration cannot be updated: Document does not exists");
    	    }
            
		} catch (DynamoDbException e) {
			log.error("patchUserConfiguration",e);
			throw new RepositoryManagerException();
		}
	}

	public void deleteUserConfiguration(String name) {

		try {
			DynamoDbTable<UserConfigurationEntity> userConfigurationTable = enhancedClient.table(
					DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME,TableSchema.fromBean(UserConfigurationEntity.class));
			UserConfigurationEntity result = userConfigurationTable.getItem(Key.builder().partitionValue(name).build());
			
            if (result == null) {
            	throw new ItemDoesNotExist(name);
            }
            else {
            	userConfigurationTable.deleteItem(result);
	            log.info("User Configuration deleted");   
            }

		} catch (DynamoDbException e) {
			log.error("deleteUserConfiguration",e);
			throw new RepositoryManagerException();

		}
	}

}

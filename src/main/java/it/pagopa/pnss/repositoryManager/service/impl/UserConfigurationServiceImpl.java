package it.pagopa.pnss.repositoryManager.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.internal.rest.v1.dto.UserConfiguration;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.constant.DynamoTableNameConstant;
import it.pagopa.pnss.repositoryManager.entity.UserConfigurationEntity;
import it.pagopa.pnss.repositoryManager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositoryManager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositoryManager.service.UserConfigurationService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Service
@Slf4j
public class UserConfigurationServiceImpl implements UserConfigurationService {

	@Autowired
	private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
	@Autowired
    private ObjectMapper objectMapper;
	
	private Mono<UserConfigurationEntity> getErrorIdClientNotFoundException(String name) {
		log.error("getErrorIdClientNotFoundException() : userConfiguration with name \"{}\" not found", name);
		return Mono.error(new IdClientNotFoundException(name));
	}

	@Override
	public Mono<UserConfiguration> getUserConfiguration(String name) {
		log.info("getUserConfiguration() : IN : name {}", name);
		
		DynamoDbAsyncTable<UserConfigurationEntity> userConfigurationTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME, TableSchema.fromBean(UserConfigurationEntity.class));
        
        return Mono.fromCompletionStage(userConfigurationTable.getItem(Key.builder().partitionValue(name).build()))
        			.switchIfEmpty(getErrorIdClientNotFoundException(name))
        			.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
        			.map(userConfigurationEntity -> objectMapper.convertValue(userConfigurationEntity, UserConfiguration.class));
	}

	@Override
	public Mono<UserConfiguration> insertUserConfiguration(UserConfiguration userConfigurationInput) {
		log.info("insertUserConfiguration() : IN : userConfigurationInput : {}", userConfigurationInput);
		
		if (userConfigurationInput == null) {
			throw new RepositoryManagerException("userConfiguration is null");
		}
		if (userConfigurationInput.getName() == null || userConfigurationInput.getName().isBlank()) {
			throw new RepositoryManagerException("userConfiguration Id is null");
		}
		
		DynamoDbAsyncTable<UserConfigurationEntity> userConfigurationTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME, TableSchema.fromBean(UserConfigurationEntity.class));
		UserConfigurationEntity documentEntityInput = objectMapper.convertValue(userConfigurationInput, UserConfigurationEntity.class);
        
        return Mono.fromCompletionStage(userConfigurationTable.getItem(Key.builder().partitionValue(userConfigurationInput.getName()).build()))
        		.doOnSuccess( userConfigurationFounded -> {
            		if (userConfigurationFounded != null) {
            			log.error("insertUserConfiguration() : userConfiguration founded : {}", userConfigurationFounded);
            			 throw new ItemAlreadyPresent(userConfigurationFounded.getApiKey());
            		}
            	})
        		.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
	        	// Puts a single item in the mapped table. 
	        	// If the table contains an item with the same primary key, it will be replaced with this item. 
	        	.doOnSuccess(unused -> userConfigurationTable.putItem(builder -> builder.item(documentEntityInput)))
	        	.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
	        	.map(documentEntity -> objectMapper.convertValue(documentEntity, UserConfiguration.class));
	}

	@Override
	public Mono<UserConfiguration> patchUserConfiguration(String name, UserConfiguration userConfigurationInput) {
		log.info("patchUserConfiguration() : IN : name : {} , userConfigurationInput {}", name, userConfigurationInput);
		
		DynamoDbAsyncTable<UserConfigurationEntity> userConfigurationTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME, TableSchema.fromBean(UserConfigurationEntity.class));
		UserConfigurationEntity userConfigurationEntityInput = objectMapper.convertValue(userConfigurationInput, UserConfigurationEntity.class);

        return Mono.fromCompletionStage(userConfigurationTable.getItem(Key.builder().partitionValue(name).build()))
        		.switchIfEmpty(getErrorIdClientNotFoundException(name))
        		.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
                .doOnSuccess(entityStored -> {
                	if (entityStored.getCanCreate() != null && !entityStored.getCanCreate().isEmpty()
                			&& userConfigurationEntityInput.getCanCreate() != null && !userConfigurationEntityInput.getCanCreate().isEmpty()) {
                		userConfigurationEntityInput.getCanCreate().forEach(can -> {
                			if (!entityStored.getCanCreate().contains(can)) {
                				entityStored.getCanCreate().add(can);
                			}
                		});
                	}
                	if (entityStored.getCanRead() != null && !entityStored.getCanRead().isEmpty()
                			&& userConfigurationEntityInput.getCanRead() != null && !userConfigurationEntityInput.getCanRead().isEmpty()) {
                		userConfigurationEntityInput.getCanRead().forEach(can -> {
                			if (!entityStored.getCanRead().contains(can)) {
                				entityStored.getCanRead().add(can);
                			}
                		});
                	}
                	log.info("patchUserConfiguration() : userConfigurationEntity for patch : {}", entityStored);
                	// Updates an item in the mapped table, or adds it if it doesn't exist. 
                	userConfigurationTable.updateItem(entityStored);
                })
                .doOnError(throwable -> log.error(throwable.getMessage(), throwable))
                .map(userConfigurationEntity -> objectMapper.convertValue(userConfigurationEntity, UserConfiguration.class));
	}

	@Override
	public Mono<UserConfiguration> deleteUserConfiguration(String name) {
		log.info("deleteUserConfiguration() : IN : name {}", name);

		DynamoDbAsyncTable<UserConfigurationEntity> userConfigurationTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME, TableSchema.fromBean(UserConfigurationEntity.class));
        Key userConfigurationKey = Key.builder().partitionValue(name).build();
        
        return Mono.fromCompletionStage(userConfigurationTable.getItem(userConfigurationKey))
        		.switchIfEmpty(getErrorIdClientNotFoundException(name))
        		.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
        		.doOnSuccess(unused -> userConfigurationTable.deleteItem(userConfigurationKey))
        		.doOnError(throwable -> log.error(throwable.getMessage(), throwable))
        		.map(userConfigurationEntity -> objectMapper.convertValue(userConfigurationEntity, UserConfiguration.class));
	}

}

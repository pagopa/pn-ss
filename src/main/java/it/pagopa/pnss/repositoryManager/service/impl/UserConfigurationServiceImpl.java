package it.pagopa.pnss.repositoryManager.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.internal.rest.v1.dto.UserConfiguration;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.constant.DynamoTableNameConstant;
import it.pagopa.pnss.repositoryManager.entity.UserConfigurationEntity;
import it.pagopa.pnss.repositoryManager.exception.ItemAlreadyPresent;
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

	@Override
	public Mono<UserConfiguration> getUserConfiguration(String name) {
		
		DynamoDbAsyncTable<UserConfigurationEntity> userConfigurationTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME, TableSchema.fromBean(UserConfigurationEntity.class));
        
        Mono<UserConfigurationEntity> monoEntity = Mono.fromCompletionStage(userConfigurationTable.getItem(Key.builder().partitionValue(name).build()))
        			.switchIfEmpty(Mono.error(new IdClientNotFoundException(name)))
        			.doOnError(IdClientNotFoundException.class, throwable -> log.info(throwable.getMessage()));
        
        return monoEntity.map(userConfigurationEntity -> objectMapper.convertValue(userConfigurationEntity, UserConfiguration.class));
	}

	@Override
	public Mono<UserConfiguration> insertUserConfiguration(UserConfiguration userConfigurationInput) {
		
		DynamoDbAsyncTable<UserConfigurationEntity> userConfigurationTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME, TableSchema.fromBean(UserConfigurationEntity.class));
		UserConfigurationEntity documentEntityInput = objectMapper.convertValue(userConfigurationInput, UserConfigurationEntity.class);
        
        Mono<UserConfigurationEntity> monoEntity = Mono.fromCompletionStage(userConfigurationTable.getItem(Key.builder().partitionValue(userConfigurationInput.getName()).build()))
        	.handle( (docTypeFounded, sink) -> {
        		if (docTypeFounded != null) {
        			sink.error(new ItemAlreadyPresent(userConfigurationInput.getName()));
        		}
        	})
        	.doOnError(ItemAlreadyPresent.class, throwable -> log.info(throwable.getMessage()))
        	// Puts a single item in the mapped table. 
        	// If the table contains an item with the same primary key, it will be replaced with this item. 
        	.doOnSuccess(unused -> userConfigurationTable.putItem(builder -> builder.item(documentEntityInput)))
        	.thenReturn(documentEntityInput);
        
        return monoEntity.map(documentEntity -> objectMapper.convertValue(documentEntity, UserConfiguration.class));
	}

	@Override
	public Mono<UserConfiguration> patchUserConfiguration(String name, UserConfiguration userConfigurationInput) {
		
		DynamoDbAsyncTable<UserConfigurationEntity> userConfigurationTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME, TableSchema.fromBean(UserConfigurationEntity.class));
		UserConfigurationEntity userConfigurationEntityInput = objectMapper.convertValue(userConfigurationInput, UserConfigurationEntity.class);

        Mono<UserConfigurationEntity> monoEntity = Mono.fromCompletionStage(userConfigurationTable.getItem(Key.builder().partitionValue(name).build()))
        		.switchIfEmpty(Mono.error(new IdClientNotFoundException(name)))
        		.doOnError(IdClientNotFoundException.class, throwable -> log.info(throwable.getMessage()))
                .doOnSuccess(entityStored -> {
                	userConfigurationEntityInput.setName(name);
                	
                	if (entityStored.getCanCreate() != null && !entityStored.getCanCreate().isEmpty()
                			&& userConfigurationEntityInput.getCanCreate() != null && !userConfigurationEntityInput.getCanCreate().isEmpty()) {
                		userConfigurationEntityInput.getCanCreate().addAll(entityStored.getCanCreate());
                	}
                	if (entityStored.getCanRead() != null && !entityStored.getCanRead().isEmpty()
                			&& userConfigurationEntityInput.getCanRead() != null && !userConfigurationEntityInput.getCanRead().isEmpty()) {
                		userConfigurationEntityInput.getCanRead().addAll(entityStored.getCanRead());
                	}
                	
                	userConfigurationEntityInput.setApiKey(entityStored.getApiKey());
                	userConfigurationEntityInput.setDestination(entityStored.getDestination());
                	userConfigurationEntityInput.setSignatureInfo(entityStored.getSignatureInfo());
                	
                	// Updates an item in the mapped table, or adds it if it doesn't exist. 
                	userConfigurationTable.updateItem(userConfigurationEntityInput);
                })
                .thenReturn(userConfigurationEntityInput);
        
        return monoEntity.map(userConfigurationEntity -> objectMapper.convertValue(userConfigurationEntity, UserConfiguration.class));
	}

	@Override
	public Mono<UserConfiguration> deleteUserConfiguration(String name) {

		DynamoDbAsyncTable<UserConfigurationEntity> userConfigurationTable = dynamoDbEnhancedAsyncClient.table(
        		DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME, TableSchema.fromBean(UserConfigurationEntity.class));
        Key userConfigurationKey = Key.builder().partitionValue(name).build();
        
        return Mono.fromCompletionStage(userConfigurationTable.getItem(userConfigurationKey))
        		.switchIfEmpty(Mono.error(new IdClientNotFoundException(name)))
        		.doOnError(IdClientNotFoundException.class, throwable -> log.info(throwable.getMessage()))
        		.doOnSuccess(unused -> userConfigurationTable.deleteItem(userConfigurationKey))
        		.map(userConfigurationEntity -> objectMapper.convertValue(userConfigurationEntity, UserConfiguration.class));
	}

}

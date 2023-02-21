package it.pagopa.pnss.repositorymanager.service.impl;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.internal.rest.v1.dto.UserConfiguration;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfigurationChanges;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.UserConfigurationEntity;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.UserConfigurationService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Service
@Slf4j
public class UserConfigurationServiceImpl implements UserConfigurationService {

    private final ObjectMapper objectMapper;

    private final DynamoDbAsyncTable<UserConfigurationEntity> userConfigurationEntityDynamoDbAsyncTable;

    public UserConfigurationServiceImpl(ObjectMapper objectMapper, DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                        RepositoryManagerDynamoTableName repositoryManagerDynamoTableName) {
        this.objectMapper = objectMapper;
        this.userConfigurationEntityDynamoDbAsyncTable =
                dynamoDbEnhancedAsyncClient.table(repositoryManagerDynamoTableName.anagraficaClientName(),
                                                  TableSchema.fromBean(UserConfigurationEntity.class));
    }

    private Mono<UserConfigurationEntity> getErrorIdClientNotFoundException(String name) {
        return Mono.error(new IdClientNotFoundException(name));
    }

    @Override
    public Mono<UserConfiguration> getUserConfiguration(String name) {
        log.info("getUserConfiguration() : IN : name {}", name);
        return Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(name).build()))
                   .switchIfEmpty(getErrorIdClientNotFoundException(name))
                   .doOnError(IdClientNotFoundException.class, throwable -> log.error(throwable.getMessage()))
                   .map(userConfigurationEntity -> objectMapper.convertValue(userConfigurationEntity, UserConfiguration.class));
    }

    @Override
    public Mono<UserConfiguration> insertUserConfiguration(UserConfiguration userConfigurationInput) {
        log.info("insertUserConfiguration() : IN : userConfigurationInput : {}", userConfigurationInput);

        if (userConfigurationInput == null) {
            throw new RepositoryManagerException("UserConfiguration is null");
        }
        if (userConfigurationInput.getName() == null || userConfigurationInput.getName().isBlank()) {
            throw new RepositoryManagerException("UserConfiguration Name is null");
        }

        UserConfigurationEntity userConfigurationEntity = objectMapper.convertValue(userConfigurationInput, UserConfigurationEntity.class);

        return Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.getItem(Key.builder()
                                                                                             .partitionValue(userConfigurationInput.getName())
                                                                                             .build()))
                .flatMap(foundedClientConfiguration -> Mono.error(new ItemAlreadyPresent(userConfigurationInput.getApiKey())))
                .doOnError(ItemAlreadyPresent.class, throwable -> log.info(throwable.getMessage()))
                .switchIfEmpty(Mono.just(userConfigurationInput))
                .flatMap(unused -> Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.putItem(builder -> builder.item(
                        userConfigurationEntity))))
                .thenReturn(userConfigurationInput);
    }

    @Override
    public Mono<UserConfiguration> patchUserConfiguration(String name, UserConfigurationChanges userConfigurationChanges) {
        log.info("patchUserConfiguration() : IN : name : {} , userConfigurationChanges {}", name, userConfigurationChanges);

        return Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(name).build()))
                   .switchIfEmpty(getErrorIdClientNotFoundException(name))
                   .doOnError(IdClientNotFoundException.class, throwable -> log.error(throwable.getMessage()))
                   .map(entityStored -> {
                       log.info("patchUserConfiguration() : userConfigurationEntity begore patch : {}", entityStored);
                       if (userConfigurationChanges.getCanCreate() != null && !userConfigurationChanges.getCanCreate().isEmpty()) {
                           entityStored.setCanCreate(userConfigurationChanges.getCanCreate());
                       }
                       if (userConfigurationChanges.getCanRead() != null && !userConfigurationChanges.getCanRead().isEmpty()) {
                           entityStored.setCanRead(userConfigurationChanges.getCanRead());
                       }
                       if (userConfigurationChanges.getCanModifyStatus() != null && !userConfigurationChanges.getCanModifyStatus().isEmpty()) {
                           entityStored.setCanModifyStatus(userConfigurationChanges.getCanModifyStatus());
                       }
                       if (userConfigurationChanges.getApiKey() != null && !userConfigurationChanges.getApiKey().isBlank()) {
                           entityStored.setApiKey(userConfigurationChanges.getApiKey());
                       }
                       if (userConfigurationChanges.getSignatureInfo() != null && !userConfigurationChanges.getSignatureInfo().isBlank()) {
                           entityStored.setSignatureInfo(userConfigurationChanges.getSignatureInfo());
                       }
                       log.info("patchUserConfiguration() : userConfigurationEntity for patch : {}", entityStored);
                       return entityStored;
                   })
                   .zipWhen(userConfigurationUpdated -> Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.updateItem(
                           userConfigurationUpdated)))
                   .map(objects -> objectMapper.convertValue(objects.getT2(), UserConfiguration.class));
    }

    @Override
    public Mono<UserConfiguration> deleteUserConfiguration(String name) {
        log.info("deleteUserConfiguration() : IN : name {}", name);
        Key userConfigurationKey = Key.builder().partitionValue(name).build();

        return Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.getItem(userConfigurationKey))
                   .switchIfEmpty(getErrorIdClientNotFoundException(name))
                   .doOnError(IdClientNotFoundException.class, throwable -> log.error(throwable.getMessage()))
                   .zipWhen(userConfigurationToDelete -> Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.deleteItem(
                           userConfigurationKey)))
                   .map(userConfigurationEntity -> objectMapper.convertValue(userConfigurationEntity.getT1(), UserConfiguration.class));
    }
}

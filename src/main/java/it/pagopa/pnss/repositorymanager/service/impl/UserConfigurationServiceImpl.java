package it.pagopa.pnss.repositorymanager.service.impl;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfiguration;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfigurationChanges;
import it.pagopa.pnss.common.utils.LogUtils;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.UserConfigurationEntity;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.UserConfigurationService;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import static it.pagopa.pnss.common.utils.DynamoDbUtils.DYNAMO_OPTIMISTIC_LOCKING_RETRY;

@Service
@CustomLog
public class UserConfigurationServiceImpl implements UserConfigurationService {

    private final ObjectMapper objectMapper;
    private final DynamoDbAsyncTable<UserConfigurationEntity> userConfigurationEntityDynamoDbAsyncTable;
    @Autowired
    RepositoryManagerDynamoTableName managerDynamoTableName;

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
        return Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(name).build()))
                   .switchIfEmpty(getErrorIdClientNotFoundException(name))
                   .doOnError(IdClientNotFoundException.class, throwable -> log.debug(throwable.getMessage()))
                   .map(userConfigurationEntity -> objectMapper.convertValue(userConfigurationEntity, UserConfiguration.class))
                   .doOnSuccess(userConfiguration -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, name, "UserConfigurationServiceImpl.getUserConfiguration()", userConfiguration));
    }

    @Override
    public Mono<UserConfiguration> insertUserConfiguration(UserConfiguration userConfigurationInput) {
        final String USER_CONFIGURATION_INPUT = "userConfigurationInput in UserConfigurationServiceImpl.insertUserConfiguration()";

        log.info(LogUtils.CHECKING_VALIDATION_PROCESS, USER_CONFIGURATION_INPUT);
        if (userConfigurationInput == null) {
            log.warn(LogUtils.VALIDATION_PROCESS_FAILED, USER_CONFIGURATION_INPUT, "UserConfiguration is null");
            throw new RepositoryManagerException("UserConfiguration is null");
        }
        if (userConfigurationInput.getName() == null || userConfigurationInput.getName().isBlank()) {
            log.warn(LogUtils.VALIDATION_PROCESS_FAILED, USER_CONFIGURATION_INPUT, "UserConfiguration Name is null");
            throw new RepositoryManagerException("UserConfiguration Name is null");
        }
        log.info(LogUtils.VALIDATION_PROCESS_PASSED, USER_CONFIGURATION_INPUT);

        UserConfigurationEntity userConfigurationEntity = objectMapper.convertValue(userConfigurationInput, UserConfigurationEntity.class);

        return Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.getItem(Key.builder()
                                                                                             .partitionValue(userConfigurationInput.getName())
                                                                                             .build()))
                .flatMap(foundedClientConfiguration -> Mono.error(new ItemAlreadyPresent(userConfigurationInput.getApiKey())))
                .doOnError(ItemAlreadyPresent.class, throwable -> log.debug(throwable.getMessage()))
                .switchIfEmpty(Mono.just(userConfigurationInput))
                .flatMap(unused -> {
                    log.debug(LogUtils.INSERTING_DATA_IN_DYNAMODB_TABLE, userConfigurationInput, userConfigurationEntity);
                    return Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.putItem(builder -> builder.item(
                            userConfigurationEntity)));
                })
                .doOnSuccess(unused -> {
                    log.info(LogUtils.INSERTED_DATA_IN_DYNAMODB_TABLE, userConfigurationEntity);
                    log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, userConfigurationInput.getName(), "UserConfigurationServiceImpl.insertUserConfiguration()", userConfigurationInput);
                })
                .thenReturn(userConfigurationInput);
    }

    @Override
    public Mono<UserConfiguration> patchUserConfiguration(String name, UserConfigurationChanges userConfigurationChanges) {

        return Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(name).build()))
                   .switchIfEmpty(getErrorIdClientNotFoundException(name))
                   .doOnError(IdClientNotFoundException.class, throwable -> log.debug(throwable.getMessage()))
                   .map(entityStored -> {
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
                       return entityStored;
                   })
                   .zipWhen(userConfigurationUpdated -> {
                       log.debug(LogUtils.UPDATING_DATA_IN_DYNAMODB_TABLE, userConfigurationUpdated, managerDynamoTableName.anagraficaClientName());
                       return Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.updateItem(
                               userConfigurationUpdated));
                   }).retryWhen(DYNAMO_OPTIMISTIC_LOCKING_RETRY)
                   .map(objects -> {
                       log.debug(LogUtils.UPDATED_DATA_IN_DYNAMODB_TABLE, managerDynamoTableName.anagraficaClientName());
                       return objectMapper.convertValue(objects.getT2(), UserConfiguration.class);
                   })
                   .doOnSuccess(userConfiguration -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, name, "UserConfigurationServiceImpl.patchUserConfiguration()", userConfiguration));
    }

    @Override
    public Mono<UserConfiguration> deleteUserConfiguration(String name) {
        Key userConfigurationKey = Key.builder().partitionValue(name).build();

        return Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.getItem(userConfigurationKey))
                   .switchIfEmpty(getErrorIdClientNotFoundException(name))
                   .doOnError(IdClientNotFoundException.class, throwable -> log.debug(throwable.getMessage()))
                   .zipWhen(userConfigurationToDelete -> {
                       log.debug(LogUtils.DELETING_DATA_IN_DYNAMODB_TABLE, userConfigurationKey, managerDynamoTableName.anagraficaClientName());
                       return Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.deleteItem(
                               userConfigurationKey));
                   })
                   .map(userConfigurationEntity -> {
                       log.debug(LogUtils.DELETED_DATA_IN_DYNAMODB_TABLE, managerDynamoTableName.anagraficaClientName());
                       return objectMapper.convertValue(userConfigurationEntity.getT1(), UserConfiguration.class);
                   })
                   .doOnSuccess(userConfiguration -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, name, "UserConfigurationServiceImpl.deleteUserConfiguration()", userConfiguration));
    }
}

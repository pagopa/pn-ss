package it.pagopa.pnss.repositorymanager.service.impl;

import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncClientDecorator;
import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbEnhancedAsyncClientDecorator;
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
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.stream.Stream;

import static it.pagopa.pnss.common.utils.DynamoDbUtils.DYNAMO_OPTIMISTIC_LOCKING_RETRY;
import static it.pagopa.pnss.common.utils.LogUtils.INVOKING_METHOD;

@Service
@CustomLog
public class UserConfigurationServiceImpl implements UserConfigurationService {

    private final ObjectMapper objectMapper;
    private final DynamoDbAsyncTableDecorator<UserConfigurationEntity> userConfigurationEntityDynamoDbAsyncTable;
    private final RetryBackoffSpec dynamoRetryStrategy;

    @Autowired
    RepositoryManagerDynamoTableName managerDynamoTableName;

    public UserConfigurationServiceImpl(ObjectMapper objectMapper, DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                        RepositoryManagerDynamoTableName repositoryManagerDynamoTableName, RetryBackoffSpec dynamoRetryStrategy) {
        this.objectMapper = objectMapper;
        this.userConfigurationEntityDynamoDbAsyncTable = new DynamoDbAsyncTableDecorator<>(
                dynamoDbEnhancedAsyncClient.table(repositoryManagerDynamoTableName.anagraficaClientName(),
                        TableSchema.fromBean(UserConfigurationEntity.class)));
        this.dynamoRetryStrategy = dynamoRetryStrategy;
    }

    private Mono<UserConfigurationEntity> getErrorIdClientNotFoundException(String name) {
        return Mono.error(new IdClientNotFoundException(name));
    }

    @Override
    public Mono<UserConfiguration> getUserConfiguration(String name) {
        final String GET_USER_CONFIGURATION = "UserConfigurationService.getUserConfiguration()";
        log.debug(INVOKING_METHOD, GET_USER_CONFIGURATION, name);

        return Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(name).build()))
                   .retryWhen(dynamoRetryStrategy)
                   .switchIfEmpty(getErrorIdClientNotFoundException(name))
                   .doOnError(IdClientNotFoundException.class, throwable -> log.debug(throwable.getMessage()))
                   .map(userConfigurationEntity -> objectMapper.convertValue(userConfigurationEntity, UserConfiguration.class))
                   .doOnSuccess(userConfiguration -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, GET_USER_CONFIGURATION, userConfiguration));
    }

    @Override
    public Mono<UserConfiguration> insertUserConfiguration(UserConfiguration userConfigurationInput) {
        final String INSERT_USER_CONFIGURATION = "UserConfigurationService.insertUserConfiguration()";
        log.debug(INVOKING_METHOD, INSERT_USER_CONFIGURATION, userConfigurationInput);

        final String USER_CONFIGURATION_INPUT = "UserConfigurationInput";
        log.logChecking(USER_CONFIGURATION_INPUT);
        if (userConfigurationInput == null) {
            String errorMsg = "UserConfiguration is null";
            log.logCheckingOutcome(USER_CONFIGURATION_INPUT, false, errorMsg);
            throw new RepositoryManagerException(errorMsg);
        }
        if (userConfigurationInput.getName() == null || userConfigurationInput.getName().isBlank()) {
            String errorMsg = "UserConfiguration Name is null";
            log.logCheckingOutcome(USER_CONFIGURATION_INPUT, false, errorMsg);
            throw new RepositoryManagerException(errorMsg);
        }
        log.logCheckingOutcome(USER_CONFIGURATION_INPUT, true);

        UserConfigurationEntity userConfigurationEntity = objectMapper.convertValue(userConfigurationInput, UserConfigurationEntity.class);

        return Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.getItem(Key.builder()
                                                                                             .partitionValue(userConfigurationInput.getName())
                                                                                             .build()))
                .flatMap(foundedClientConfiguration -> Mono.error(new ItemAlreadyPresent(userConfigurationInput.getApiKey())))
                .doOnError(ItemAlreadyPresent.class, throwable -> log.debug(throwable.getMessage()))
                .switchIfEmpty(Mono.just(userConfigurationInput))
                .flatMap(unused -> Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.putItem(builder -> builder.item(
                         userConfigurationEntity))).retryWhen(dynamoRetryStrategy))
                .doOnSuccess(unused -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, INSERT_USER_CONFIGURATION, userConfigurationInput))
                .thenReturn(userConfigurationInput);
    }

    @Override
    public Mono<UserConfiguration> patchUserConfiguration(String name, UserConfigurationChanges userConfigurationChanges) {
        final String PATCH_USER_CONFIGURATION = "UserConfigurationService.patchUserConfiguration()";
        log.debug(INVOKING_METHOD, PATCH_USER_CONFIGURATION, Stream.of(name, userConfigurationChanges).toList());

        return Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(name).build()))
                   .retryWhen(dynamoRetryStrategy)
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
                   .zipWhen(userConfigurationUpdated -> Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.updateItem(
                           userConfigurationUpdated))).retryWhen(dynamoRetryStrategy)
                   .map(objects -> objectMapper.convertValue(objects.getT2(), UserConfiguration.class))
                   .doOnSuccess(userConfiguration -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL,PATCH_USER_CONFIGURATION, userConfiguration));
    }

    @Override
    public Mono<UserConfiguration> deleteUserConfiguration(String name) {
        final String DELETE_USER_CONFIGURATION = "UserConfigurationService.deleteUserConfiguration()";
        log.debug(INVOKING_METHOD, DELETE_USER_CONFIGURATION, name);

        Key userConfigurationKey = Key.builder().partitionValue(name).build();

        return Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.getItem(userConfigurationKey))
                   .switchIfEmpty(getErrorIdClientNotFoundException(name))
                   .doOnError(IdClientNotFoundException.class, throwable -> log.debug(throwable.getMessage()))
                   .zipWhen(userConfigurationToDelete -> Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.deleteItem(
                            userConfigurationKey)))
                   .retryWhen(dynamoRetryStrategy)
                   .map(userConfigurationEntity -> objectMapper.convertValue(userConfigurationEntity.getT1(), UserConfiguration.class))
                   .doOnSuccess(userConfiguration -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, DELETE_USER_CONFIGURATION, userConfiguration));
    }
}

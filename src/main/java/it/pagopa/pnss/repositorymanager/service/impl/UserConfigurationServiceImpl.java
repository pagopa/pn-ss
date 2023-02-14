package it.pagopa.pnss.repositorymanager.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfiguration;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.UserConfigurationEntity;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.UserConfigurationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
        log.error("getErrorIdClientNotFoundException() : userConfiguration with name \"{}\" not found", name);
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
                   .handle((userConfigurationFounded, sink) -> {
                       if (userConfigurationFounded != null) {
                           log.error("insertUserConfiguration() : userConfiguration founded : {}", userConfigurationFounded);
                           sink.error(new ItemAlreadyPresent(userConfigurationFounded.getApiKey()));
                       }
                   })
                   .doOnError(ItemAlreadyPresent.class, throwable -> log.error(throwable.getMessage()))
                   // Puts a single item in the mapped table.
                   // If the table contains an item with the same primary key, it will be replaced with this item.
                   .switchIfEmpty(Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.putItem(builder -> builder.item(
                           userConfigurationEntity))))
                   .thenReturn(userConfigurationInput);
    }

    @Override
    public Mono<UserConfiguration> patchUserConfiguration(String name, UserConfiguration userConfigurationInput) {
        log.info("patchUserConfiguration() : IN : name : {} , userConfigurationInput {}", name, userConfigurationInput);

        UserConfigurationEntity userConfigurationEntityInput =
                objectMapper.convertValue(userConfigurationInput, UserConfigurationEntity.class);

        return Mono.fromCompletionStage(userConfigurationEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(name).build()))
                   .switchIfEmpty(getErrorIdClientNotFoundException(name))
                   .doOnError(IdClientNotFoundException.class, throwable -> log.error(throwable.getMessage()))
                   .map(entityStored -> {
                       if (entityStored.getCanCreate() != null && !entityStored.getCanCreate().isEmpty() &&
                           userConfigurationEntityInput.getCanCreate() != null && !userConfigurationEntityInput.getCanCreate().isEmpty()) {
                           userConfigurationEntityInput.getCanCreate().forEach(can -> {
                               if (!entityStored.getCanCreate().contains(can)) {
                                   entityStored.getCanCreate().add(can);
                               }
                           });
                       }
                       if (entityStored.getCanRead() != null && !entityStored.getCanRead().isEmpty() &&
                           userConfigurationEntityInput.getCanRead() != null && !userConfigurationEntityInput.getCanRead().isEmpty()) {
                           userConfigurationEntityInput.getCanRead().forEach(can -> {
                               if (!entityStored.getCanRead().contains(can)) {
                                   entityStored.getCanRead().add(can);
                               }
                           });
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

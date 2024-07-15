package it.pagopa.pnss.repositorymanager.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsDto;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsRelationsDto;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.model.pojo.IndexingTag;
import it.pagopa.pnss.configuration.IndexingConfiguration;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.repositorymanager.entity.TagsRelationsEntity;
import it.pagopa.pnss.repositorymanager.exception.IndexingLimitException;
import it.pagopa.pnss.repositorymanager.exception.TagKeyValueNotPresentException;
import it.pagopa.pnss.repositorymanager.service.TagsService;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;

import java.util.List;
import java.util.Map;

import static it.pagopa.pnss.common.utils.DynamoDbUtils.DYNAMO_OPTIMISTIC_LOCKING_RETRY;
import static it.pagopa.pnss.common.utils.LogUtils.*;

/**
 * A service for managing tags on pn-SsDocumenti and pn-SsTags tables.
 */
@Service
@CustomLog
public class TagsServiceImpl implements TagsService {

    private final ObjectMapper objectMapper;
    private final DynamoDbAsyncTableDecorator<TagsRelationsEntity> tagsEntityDynamoDbAsyncTable;
    private final DynamoDbAsyncTableDecorator<DocumentEntity> documentEntityDynamoDbAsyncTable;
    private final IndexingConfiguration indexingConfiguration;

    private enum OperationType {
        /**
         * Set operation type.
         */
        SET,
        /**
         * Delete operation type.
         */
        DELETE
    }

    /**
     * Instantiates a new Tags service.
     *
     * @param objectMapper                     the object mapper
     * @param dynamoDbEnhancedAsyncClient      the dynamo db enhanced async client
     * @param repositoryManagerDynamoTableName the repository manager dynamo table name
     * @param indexingConfiguration            the indexing configuration
     */
    public TagsServiceImpl(ObjectMapper objectMapper, DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient, RepositoryManagerDynamoTableName repositoryManagerDynamoTableName, IndexingConfiguration indexingConfiguration) {
        this.objectMapper = objectMapper;
        this.indexingConfiguration = indexingConfiguration;
        this.tagsEntityDynamoDbAsyncTable = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedAsyncClient.table(repositoryManagerDynamoTableName.tagsName(), TableSchema.fromBean(TagsRelationsEntity.class)));
        this.documentEntityDynamoDbAsyncTable = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedAsyncClient.table(repositoryManagerDynamoTableName.documentiName(), TableSchema.fromBean(DocumentEntity.class)));
    }

    private Mono<TagsRelationsEntity> getErrorIdTagNotFoundException(String tagKeyValue) {
        return Mono.error(new TagKeyValueNotPresentException(tagKeyValue));
    }

    /**
     * A method to get tags relations from pn-SsTags table.
     *
     * @param tagKeyValue the pn-SsTags partitionKey, representing a tag key~value association.
     * @return Mono<TagsRelationsDto> a mono with the fileKeys associated to the given tag key~value association
     * @throws TagKeyValueNotPresentException if the tag key~value association does not exist
     */
    @Override
    public Mono<TagsRelationsDto> getTagsRelations(String tagKeyValue) {
        log.debug(LogUtils.INVOKING_METHOD, GET_TAGS_RELATIONS, tagKeyValue);
        return Mono.fromCompletionStage(tagsEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(tagKeyValue).build()))
                .switchIfEmpty(getErrorIdTagNotFoundException(tagKeyValue))
                .doOnError(throwable -> log.debug(throwable.getMessage()))
                .map(tagsRelationsEntity -> objectMapper.convertValue(tagsRelationsEntity, TagsRelationsDto.class))
                .doOnSuccess(result -> log.debug(SUCCESSFUL_OPERATION_LABEL, GET_TAGS_RELATIONS, result));
    }

    /**
     * A method to update tags on pn-SsDocumenti table and related associations on pn-SsTags.
     *
     * @param documentKey the document key of the document to update tags
     * @param tagsChanges the changes to perform on the tags
     * @return Mono<TagsDto> a mono with the updated tags
     */
    @Override
    public Mono<TagsDto> putTags(String documentKey, TagsChanges tagsChanges) {
        log.debug(LogUtils.INVOKING_METHOD, PUT_TAGS, List.of(documentKey, tagsChanges));
        var setFlux = Mono.justOrEmpty(tagsChanges.getSET()).map(Map::entrySet).flatMapMany(Flux::fromIterable).map(setEntry -> Tuples.of(setEntry, OperationType.SET));
        var deleteFlux = Mono.justOrEmpty(tagsChanges.getDELETE()).map(Map::entrySet).flatMapMany(Flux::fromIterable).map(deleteEntry -> Tuples.of(deleteEntry, OperationType.DELETE));
        // Setting up a flux with set and delete operations
        var operationsFlux = Flux.merge(setFlux, deleteFlux);
        // Tags updating
        return updateTags(documentKey, operationsFlux).retryWhen(DYNAMO_OPTIMISTIC_LOCKING_RETRY)
                // Relations updating
                .flatMap(documentEntity -> updateRelations(documentKey, operationsFlux).thenReturn(documentEntity.getTags() == null ? new HashMap<String, List<String>>() : documentEntity.getTags()))
                .map(tags -> new TagsDto().tags(tags))
                .doOnSuccess(result -> log.debug(SUCCESSFUL_OPERATION_LABEL, PUT_TAGS, result));
    }



    private Mono<DocumentEntity> setTags(DocumentEntity documentEntity, Map<String, List<String>> set) {
        log.debug("setTags: {}", set);
        return Mono.justOrEmpty(documentEntity.getTags())
                .defaultIfEmpty(new HashMap<>())
                .filter(tags -> set != null && !set.isEmpty())
                .flatMap(tags -> handleTagSetting(tags, set))
                .doOnNext(documentEntity::setTags)
                .thenReturn(documentEntity);
}
    /**
     * A method to update tags on pn-SsDocumenti table.
     *
     * @param documentKey    the document key of the document to update tags
     * @param operationsFlux a flux containing the operations to perform
     * @return Mono<DocumentEntity> the updated document entity
     */
    private Mono<DocumentEntity> updateTags(String documentKey, Flux<Tuple2<Map.Entry<String, List<String>>, TagsServiceImpl.OperationType>> operationsFlux) {
        log.debug(LogUtils.INVOKING_METHOD, UPDATE_TAGS, documentKey);
        return Mono.fromCompletionStage(() -> documentEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(documentKey).build()))
                .switchIfEmpty(Mono.error(new DocumentKeyNotPresentException(documentKey)))
                .flatMap(documentEntity -> Mono.justOrEmpty(documentEntity.getTags())
                        .defaultIfEmpty(new HashMap<>())
                        .flatMap(tags -> getUpdatedMap(tags, operationsFlux))
                        .flatMap(updatedTags -> {
                            documentEntity.setTags(updatedTags);
                            return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.putItem(documentEntity));
                        })
                        .thenReturn(documentEntity))
                .doOnSuccess(result -> log.debug(SUCCESSFUL_OPERATION_LABEL, UPDATE_TAGS, result));

    }

    /**
     * A method to apply given operations on a map of tags.
     *
     * @param existingTags   the existing tags map to update
     * @param operationsFlux the flux containing the operations to perform
     * @return Mono<Map < String, List < String>>> a mono containing the updated tags map
     */
    private Mono<Map<String, List<String>>> getUpdatedMap(Map<String, List<String>> existingTags, Flux<Tuple2<Map.Entry<String, List<String>>, TagsServiceImpl.OperationType>> operationsFlux) {
        return operationsFlux.reduce(existingTags, (tags, tuple) -> {
            var setEntry = tuple.getT1();
            var operationType = tuple.getT2();
            String tagKey = setEntry.getKey();
            IndexingTag tagInfo = indexingConfiguration.getTagInfo(tagKey);
            if (operationType == OperationType.SET) {
                return setTag(tagInfo, tags, setEntry);
            } else {
                return deleteTag(tagInfo, tags, setEntry);
            }
        });
    }

    /**
     * A method to apply a SET operation on a given tag contained in a map.
     *
     * @param tagInfo  the tag to update related info
     * @param tags     the tags map
     * @param setEntry the entry with the tag key and the list of values to set
     * @return Map<String, List < String>> the updated tags map
     */
    private Map<String, List<String>> setTag(IndexingTag tagInfo, Map<String, List<String>> tags, Map.Entry<String, List<String>> setEntry) {
        log.debug(LogUtils.INVOKING_METHOD, SET_TAG, List.of(tagInfo, tags, setEntry));
        String tagKey = tagInfo.getKey();
        if (tagInfo.isMultivalue()) {
            HashSet<String> existingValues = tags.containsKey(tagKey) ? new HashSet<>(tags.get(tagKey)) : new HashSet<>();
            HashSet<String> newValues = new HashSet<>(setEntry.getValue());
            existingValues.addAll(newValues);
            if (existingValues.size() > indexingConfiguration.getIndexingLimits().getMaxValuesPerTagDocument()) {
                throw new IndexingLimitException("MaxValuesPerTagDocument", existingValues.size(), indexingConfiguration.getIndexingLimits().getMaxValuesPerTagDocument());
            }
            tags.put(tagInfo.getKey(), existingValues.stream().toList());
        } else {
            tags.put(tagInfo.getKey(), setEntry.getValue());
        }
        if (tags.size() > indexingConfiguration.getIndexingLimits().getMaxTagsPerDocument()) {
            throw new IndexingLimitException("MaxTagsPerDocument", tags.size(), indexingConfiguration.getIndexingLimits().getMaxTagsPerDocument());
        }
        return tags;
    }

    /**
     * A method to apply a DELETE operation on a given tag contained in a map.
     *
     * @param tagInfo  the tag to update related info
     * @param tags     the tags map
     * @param setEntry the entry with the tag key and the list of values to delete
     * @return Map<String, List < String>> the updated tags map
     */
    private Map<String, List<String>> deleteTag(IndexingTag tagInfo, Map<String, List<String>> tags, Map.Entry<String, List<String>> setEntry) {
        log.debug(LogUtils.INVOKING_METHOD, DELETE_TAG, List.of(tagInfo, tags, setEntry));
        String tagKey = tagInfo.getKey();
        if (tags.containsKey(tagKey)) {
            var tagValues = tags.get(tagKey);
            if (tagValues != null) {
                tagValues.removeAll(setEntry.getValue());
            }
            tags.put(tagInfo.getKey(), tagValues);
        }
        return tags;
    }

    /**
     * A method to update the relations on the pn-SsTags table, based on given operations.
     *
     * @param fileKey        the fileKey to add to the list of related fileKeys
     * @param operationsFlux a flux containing the operations
     * @return Mono<Void> a void mono
     */
    private Mono<Void> updateRelations(String fileKey, Flux<Tuple2<Map.Entry<String, List<String>>, TagsServiceImpl.OperationType>> operationsFlux) {
        log.debug(LogUtils.INVOKING_METHOD, UPDATE_RELATIONS, fileKey);
        return operationsFlux.filter(tuple -> indexingConfiguration.getTagInfo(tuple.getT1().getKey()).isIndexed())
                .flatMap(tuple -> {
                    var entrySet = tuple.getT1();
                    String tagKey = entrySet.getKey();
                    OperationType operationType = tuple.getT2();
                    return Flux.fromIterable(entrySet.getValue())
                            .flatMap(tagValue -> updateRelation(tagKey + "~" + tagValue, operationType, fileKey))
                            .retryWhen(DYNAMO_OPTIMISTIC_LOCKING_RETRY);
                })
                .then()
                .doOnSuccess(ignored -> log.debug(SUCCESSFUL_OPERATION_LABEL_NO_ARGS, UPDATE_RELATIONS));
    }

    /**
     * Update a single relation.
     * This method uses condition expression based updated to handle concurrent updates and optimistic locking.
     *
     * @param tagKeyValue   the pn-SsTags partitionKey, representing a tag key~value association.
     * @param operationType the operation type
     * @param fileKey       the file key to add to the list of related fileKeys
     * @return the mono
     */
    Mono<Boolean> updateRelation(String tagKeyValue, OperationType operationType, String fileKey) {
        return Mono.fromCompletionStage(() -> tagsEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(tagKeyValue))))
                .flatMap(tagsRelationsEntity -> {
                    Long currVersion = tagsRelationsEntity.getVersion();
                    // SET operation
                    if (operationType == OperationType.SET) {
                        log.debug("Updating fileKeys list...");
                        var fileKeysSet = new HashSet<>(tagsRelationsEntity.getFileKeys());
                        fileKeysSet.add(fileKey);
                        if (fileKeysSet.size() > indexingConfiguration.getIndexingLimits().getMaxFileKeys()) {
                            throw new IndexingLimitException("MaxFileKeys", fileKeysSet.size(), indexingConfiguration.getIndexingLimits().getMaxFileKeys());
                        }
                        tagsRelationsEntity.setFileKeys(fileKeysSet.stream().toList());
                    }
                    // DELETE operation
                    else {
                        tagsRelationsEntity.getFileKeys().remove(fileKey);
                        if (tagsRelationsEntity.getFileKeys().isEmpty()) {
                            return deleteIfVersionIsCorrect(tagKeyValue, currVersion);
                        }
                    }
                    return putIfExists(tagsRelationsEntity);
                })
                // INSERT MODE
                .switchIfEmpty(putIfNotExists(tagKeyValue, fileKey));
    }

    /**
     * A method to put an item in the pn-SsTags table, if it not exists.
     *
     * @param tagsRelationsEntity the item to put
     * @return Mono<Boolean>
     */
    private Mono<Boolean> putIfExists(TagsRelationsEntity tagsRelationsEntity) {
        log.debug("Update mode with TagsRelationsEntity {}", tagsRelationsEntity);
        return Mono.fromCompletionStage(tagsEntityDynamoDbAsyncTable.putItem(builder -> builder.item(tagsRelationsEntity).conditionExpression(Expression.builder().expression("attribute_exists(tagKeyValue)").build()))).thenReturn(true);
    }

    /**
     * A method to put an item in the pn-SsTags table, if it not exists.
     *
     * @param tagKeyValue the partition key of the item
     * @param fileKey     the fileKey to add to the list of related fileKeys
     * @return Mono<Boolean>
     */
    private Mono<Boolean> putIfNotExists(String tagKeyValue, String fileKey) {
        log.debug("Insert mode on tagKeyValue {} with fileKey {}", tagKeyValue, fileKey);
        return Mono.fromCompletionStage(tagsEntityDynamoDbAsyncTable.putItem(builder -> builder.item(TagsRelationsEntity.builder().tagKeyValue(tagKeyValue).fileKeys(List.of(fileKey)).build()).conditionExpression(Expression.builder().expression("attribute_not_exists(tagKeyValue)").build()))).thenReturn(true);
    }

    /**
     * A method to delete an item from the pn-SsTags table if the version is correct.
     *
     * @param tagKeyValue the partition key of the item
     * @param currVersion the current version of the item
     * @return Mono<Boolean>
     */
    private Mono<Boolean> deleteIfVersionIsCorrect(String tagKeyValue, Long currVersion) {
        log.debug("Delete mode on tagKeyValue {} with version {}", tagKeyValue, currVersion);
        return Mono.fromCompletionStage(() -> tagsEntityDynamoDbAsyncTable.deleteItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(tagKeyValue))
                        .conditionExpression(Expression.builder().expression("#version = :version")
                                .expressionNames(Map.of("#version", "version"))
                                .expressionValues(Map.of(":version", AttributeValue.builder().n(String.valueOf(currVersion)).build())).build())))
                .thenReturn(true);
    }
}

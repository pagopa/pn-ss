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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.*;

import java.util.List;
import java.util.Map;

import static it.pagopa.pnss.common.utils.LogUtils.GET_TAGS_RELATIONS;
import static it.pagopa.pnss.common.utils.LogUtils.GET_TAGS_RELATIONS_OP;

@Service
@CustomLog
public class TagsServiceImpl implements TagsService {

    private final ObjectMapper objectMapper;
    private final DynamoDbAsyncTableDecorator<TagsRelationsEntity> tagsEntityDynamoDbAsyncTable;
    private final DynamoDbAsyncTableDecorator<DocumentEntity> documentEntityDynamoDbAsyncTable;
    private final IndexingConfiguration indexingConfiguration;

    public TagsServiceImpl(ObjectMapper objectMapper, DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient, RepositoryManagerDynamoTableName repositoryManagerDynamoTableName, IndexingConfiguration indexingConfiguration) {
        this.objectMapper = objectMapper;
        this.indexingConfiguration = indexingConfiguration;
        this.tagsEntityDynamoDbAsyncTable = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedAsyncClient.table(repositoryManagerDynamoTableName.tagsName(), TableSchema.fromBean(TagsRelationsEntity.class)));
        this.documentEntityDynamoDbAsyncTable = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedAsyncClient.table(repositoryManagerDynamoTableName.documentiName(), TableSchema.fromBean(DocumentEntity.class)));
    }

    private Mono<TagsRelationsEntity> getErrorIdTagNotFoundException(String tagKeyValue) {
        return Mono.error(new TagKeyValueNotPresentException(tagKeyValue));
    }

    @Override
    public Mono<TagsRelationsDto> getTagsRelations(String tagKeyValue) {
        log.debug(LogUtils.INVOKING_METHOD, GET_TAGS_RELATIONS, tagKeyValue);
        return Mono.fromCompletionStage(tagsEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(tagKeyValue).build()))
                .switchIfEmpty(getErrorIdTagNotFoundException(tagKeyValue))
                .doOnError(throwable -> log.debug(throwable.getMessage()))
                .map(tagsRelationsEntity -> objectMapper.convertValue(tagsRelationsEntity, TagsRelationsDto.class));
    }

    @Override
    public Mono<TagsDto> updateTags(String documentKey, TagsChanges tagsChanges) {
        log.debug(LogUtils.INVOKING_METHOD, GET_TAGS_RELATIONS, List.of(documentKey, tagsChanges));
        return Mono.fromCompletionStage(() -> documentEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(documentKey).build()))
                .switchIfEmpty(Mono.error(new DocumentKeyNotPresentException(documentKey)))
                .flatMap(documentEntity -> setTags(documentEntity, tagsChanges.getSET()))
                .flatMap(documentEntity -> deleteTags(documentEntity, tagsChanges.getDELETE()))
                .flatMap(documentEntity -> Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.putItem(documentEntity)).thenReturn(documentEntity))
                .flatMap(documentEntity -> updateRelations(tagsChanges.getSET(), tagsChanges.getDELETE(), documentKey).thenReturn(documentEntity.getTags() == null ? new HashMap<String, List<String>>() : documentEntity.getTags()))
                .map(tags -> new TagsDto().tags(tags));
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

    private Mono<Map<String, List<String>>> handleTagSetting(Map<String, List<String>> existingTags, Map<String, List<String>> set) {
        return Flux.fromIterable(set.entrySet()).reduce(existingTags, (tags, setEntry) -> {
            String tagKey = setEntry.getKey();
            IndexingTag tagInfo = indexingConfiguration.getTagInfo(tagKey);
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
        });
    }

    private Mono<DocumentEntity> deleteTags(DocumentEntity doc, Map<String, List<String>> delete) {
        log.debug("deleteTags: {}", delete);
        return Mono.just(doc).flatMap(documentEntity -> {
            var tags = documentEntity.getTags();
            if (tags == null) {
                tags = new HashMap<>();
            }
            var finalTags = tags;
            if (delete != null && !delete.isEmpty()) {
                return Flux.fromIterable(delete.entrySet()).doOnNext(setEntry -> {
                            var tagInfo = indexingConfiguration.getTagInfo(setEntry.getKey());
                            if (finalTags.containsKey(tagInfo.getKey())) {
                                var tagValues = finalTags.get(tagInfo.getKey());
                                if (tagValues != null) {
                                    tagValues.removeAll(setEntry.getValue());
                                }
                                finalTags.put(tagInfo.getKey(), tagValues);
                                documentEntity.setTags(finalTags);
                            }
                        })
                        .then()
                        .thenReturn(documentEntity);
            } else return Mono.just(doc);
        });
    }

    private Mono<Void> updateRelations(Map<String, List<String>> set, Map<String, List<String>> delete, String fileKey) {
        log.debug("updateRelations: {} {} {}", set, delete, fileKey);
        return insertRelations(set, fileKey).then(deleteRelations(delete, fileKey));
    }

    private Mono<Void> insertRelations(Map<String, List<String>> set, String fileKey) {
        log.debug("insertRelations: {}", set);
        if (set != null && !set.isEmpty()) {
            return Flux.fromIterable(set.entrySet())
                    .filter(setEntry -> indexingConfiguration.getTagInfo(setEntry.getKey()).isIndexed())
                    .flatMap(setEntry -> Flux.fromIterable(setEntry.getValue()).map(value -> setEntry.getKey() + "~" + value))
                    .flatMapSequential(tagKeyValue -> {
                        log.debug("Getting record with tagKeyValue: {}", tagKeyValue);
                        return Mono.fromCompletionStage(() -> tagsEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(tagKeyValue))))
                                .defaultIfEmpty(TagsRelationsEntity.builder().tagKeyValue(tagKeyValue).fileKeys(new ArrayList<>()).build())
                                .doOnError(e -> log.error("Error getting record with tagKeyValue: {}", tagKeyValue, e))
                                .flatMap(tagsRelationsEntity -> {
                                    log.debug("updating fileKeys list...");
                                    var fileKeysSet = new HashSet<>(tagsRelationsEntity.getFileKeys());
                                    fileKeysSet.add(fileKey);
                                    if (fileKeysSet.size() > indexingConfiguration.getIndexingLimits().getMaxFileKeys()) {
                                        throw new IndexingLimitException("MaxFileKeys", fileKeysSet.size(), indexingConfiguration.getIndexingLimits().getMaxFileKeys());
                                    }
                                    tagsRelationsEntity.setFileKeys(fileKeysSet.stream().toList());
                                    return Mono.fromCompletionStage(tagsEntityDynamoDbAsyncTable.putItem(tagsRelationsEntity));
                                });
                    })
                    .then();

        }
        return Mono.empty();
    }

    private Mono<Void> deleteRelations(Map<String, List<String>> delete, String fileKey) {
        log.debug("deleteRelations: {}", delete);
        if (delete != null && !delete.isEmpty()) {
            return Flux.fromIterable(delete.entrySet())
                    .filter(setEntry -> indexingConfiguration.getTagInfo(setEntry.getKey()).isIndexed())
                    .flatMap(setEntry -> Flux.fromIterable(setEntry.getValue()).map(value -> setEntry.getKey() + "~" + value))
                    .flatMapSequential(tagKeyValue -> {
                        log.debug("Getting record with tagKeyValue: {}", tagKeyValue);
                        return Mono.fromCompletionStage(tagsEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(tagKeyValue))))
                                .flatMap(tagsRelationsEntity -> {
                                    tagsRelationsEntity.getFileKeys().remove(fileKey);
                                    if (tagsRelationsEntity.getFileKeys().isEmpty()) {
                                        return Mono.fromCompletionStage(() -> tagsEntityDynamoDbAsyncTable.deleteItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(tagKeyValue))));
                                    }
                                    return Mono.fromCompletionStage(() -> tagsEntityDynamoDbAsyncTable.putItem(tagsRelationsEntity));
                                });
                    })
                    .then();
        }
        return Mono.empty();
    }
}

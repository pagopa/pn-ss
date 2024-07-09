package it.pagopa.pnss.repositorymanager.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsDto;
import it.pagopa.pnss.configuration.IndexingConfiguration;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.repositorymanager.entity.TagsEntity;
import it.pagopa.pnss.repositorymanager.exception.IndexingLimitException;
import it.pagopa.pnss.repositorymanager.exception.TagKeyNotPresentException;
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
import static it.pagopa.pnss.common.utils.LogUtils.GET_TAGS;
@Service
@CustomLog
public class TagsServiceImpl implements TagsService {

    private final ObjectMapper objectMapper;
    private final DynamoDbAsyncTableDecorator<TagsEntity> tagsEntityDynamoDbAsyncTable;
    private final DynamoDbAsyncTableDecorator<DocumentEntity> documentEntityDynamoDbAsyncTable;
    private final IndexingConfiguration indexingConfiguration;

    public TagsServiceImpl(ObjectMapper objectMapper, DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient, RepositoryManagerDynamoTableName repositoryManagerDynamoTableName, IndexingConfiguration indexingConfiguration) {
        this.objectMapper = objectMapper;
        this.indexingConfiguration = indexingConfiguration;
        this.tagsEntityDynamoDbAsyncTable = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedAsyncClient.table(repositoryManagerDynamoTableName.tagsName(), TableSchema.fromBean(TagsEntity.class)));
        this.documentEntityDynamoDbAsyncTable = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedAsyncClient.table(repositoryManagerDynamoTableName.documentiName(), TableSchema.fromBean(DocumentEntity.class)));
    }

    private Mono<TagsEntity> getErrorIdTagNotFoundException(String tagKeyValue) {
        return Mono.error(new TagKeyNotPresentException(tagKeyValue));
    }

    @Override
    public Mono<TagsDto> getTags(String tagKeyValue) {
        log.debug(LogUtils.INVOKING_METHOD, GET_TAGS, tagKeyValue);
        return Mono.fromCompletionStage(tagsEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(tagKeyValue).build()))
                .switchIfEmpty(getErrorIdTagNotFoundException(tagKeyValue))
                .doOnError(Exception.class, throwable -> log.debug(throwable.getMessage()))
                .map(tagsEntity -> objectMapper.convertValue(tagsEntity, TagsDto.class));
    }

    @Override
    public Mono<Map<String, List<String>>> updateTags(TagsChanges tagsChanges) {
        return Mono.defer(() -> {
            log.debug("updateTags: {}", tagsChanges);
            var fileKey = tagsChanges.getFileKey();
            var set = tagsChanges.getSET();
            var delete = tagsChanges.getDELETE();
            return Mono.fromCompletionStage(() -> documentEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(fileKey).build()))
                    .flatMap(documentEntity -> setTags(documentEntity, set))
                    .flatMap(documentEntity -> deleteTags(documentEntity, delete))
                    .flatMap(documentEntity -> Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.putItem(documentEntity)).thenReturn(documentEntity))
                    .flatMap(documentEntity -> updateRelations(set, delete, fileKey).thenReturn(documentEntity.getTags() == null ? new HashMap<>() : documentEntity.getTags()));
        });

    }

    private Mono<DocumentEntity> setTags(DocumentEntity doc, Map<String, List<String>> set) {
        log.debug("setTags: {}", set);
        return Mono.just(doc).flatMap(documentEntity -> {
            var tags = documentEntity.getTags();
            if (tags == null) {
                tags = new HashMap<>();
            }
            var finalTags = tags;
            if (set != null && !set.isEmpty()) {
                return Flux.fromIterable(set.entrySet()).doOnNext(setEntry -> {
                            var tagInfo = indexingConfiguration.getTagInfo(setEntry.getKey());
                            if (finalTags.containsKey(tagInfo.getKey())) {
                                var tagValues = finalTags.get(tagInfo.getKey());
                                if (tagInfo.isMultivalue()) {
                                    var existingValuesSet = new HashSet<>(tagValues);
                                    var newValuesSet = new HashSet<>(setEntry.getValue());
                                    existingValuesSet.addAll(newValuesSet);
                                    if (existingValuesSet.size() > indexingConfiguration.getIndexingLimits().getMaxValuesPerTagDocument()) {
                                        throw new IndexingLimitException("MaxValuesPerTagDocument", existingValuesSet.size(), indexingConfiguration.getIndexingLimits().getMaxValuesPerTagDocument());
                                    }
                                    finalTags.put(tagInfo.getKey(), existingValuesSet.stream().toList());
                                    documentEntity.setTags(finalTags);
                                } else {
                                    finalTags.put(tagInfo.getKey(), setEntry.getValue());
                                    documentEntity.setTags(finalTags);
                                }
                            } else {
                                finalTags.put(tagInfo.getKey(), setEntry.getValue());
                                documentEntity.setTags(finalTags);
                            }
                            if (finalTags.size() > indexingConfiguration.getIndexingLimits().getMaxTagsPerDocument()) {
                                throw new IndexingLimitException("MaxValuesPerTagDocument", finalTags.size(), indexingConfiguration.getIndexingLimits().getMaxTagsPerDocument());
                            }
                        })
                        .then()
                        .thenReturn(documentEntity);
            } else return Mono.just(doc);
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
                                .defaultIfEmpty(TagsEntity.builder().tagKeyValue(tagKeyValue).fileKeys(new ArrayList<>()).build())
                                .doOnError(e -> log.error("Error getting record with tagKeyValue: {}", tagKeyValue, e))
                                .flatMap(tagsEntity -> {
                                    log.debug("updating fileKeys list...");
                                    var fileKeysSet = new HashSet<>(tagsEntity.getFileKeys());
                                    fileKeysSet.add(fileKey);
                                    if (fileKeysSet.size() > indexingConfiguration.getIndexingLimits().getMaxFileKeys()) {
                                        throw new IndexingLimitException("MaxFileKeys", fileKeysSet.size(), indexingConfiguration.getIndexingLimits().getMaxFileKeys());
                                    }
                                    tagsEntity.setFileKeys(fileKeysSet.stream().toList());
                                    return Mono.fromCompletionStage(tagsEntityDynamoDbAsyncTable.putItem(tagsEntity));
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
                                .flatMap(tagsEntity -> {
                                    tagsEntity.getFileKeys().remove(fileKey);
                                    if (tagsEntity.getFileKeys().isEmpty()) {
                                        return Mono.fromCompletionStage(() -> tagsEntityDynamoDbAsyncTable.deleteItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(tagKeyValue))));
                                    }
                                    return Mono.fromCompletionStage(() -> tagsEntityDynamoDbAsyncTable.putItem(tagsEntity));
                                });
                    })
                    .then();
        }
        return Mono.empty();
    }
}

package it.pagopa.pnss.repositorymanager.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Document;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsDto;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.repositorymanager.entity.TagsEntity;
import it.pagopa.pnss.repositorymanager.exception.TagKeyNotPresentException;
import it.pagopa.pnss.repositorymanager.service.TagsService;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.Map;
import static it.pagopa.pnss.common.utils.LogUtils.GET_TAGS;
@Service
@CustomLog
public class TagsServiceImpl implements TagsService {

    private final ObjectMapper objectMapper;
    private final DynamoDbAsyncTableDecorator<TagsEntity> tagsEntityDynamoDbAsyncTable;
    private final DynamoDbAsyncTableDecorator<DocumentEntity> documentEntityDynamoDbAsyncTable;

    public TagsServiceImpl(ObjectMapper objectMapper, DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient, RepositoryManagerDynamoTableName repositoryManagerDynamoTableName) {
        this.objectMapper = objectMapper;
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
        return null;
    }
}

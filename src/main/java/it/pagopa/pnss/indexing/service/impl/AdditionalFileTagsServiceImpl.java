package it.pagopa.pnss.indexing.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsDto;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.TagsClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configuration.IndexingConfiguration;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.indexing.service.AdditionalFileTagsService;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.repositorymanager.entity.TagsRelationsEntity;
import lombok.CustomLog;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.*;


@Service
@CustomLog
public class AdditionalFileTagsServiceImpl implements AdditionalFileTagsService {

    private final TagsClientCall tagsClientCall;
    private final ObjectMapper objectMapper;
    private final DynamoDbAsyncTableDecorator<TagsRelationsEntity> tagsEntityDynamoDbAsyncTable;
    private final DynamoDbAsyncTableDecorator<DocumentEntity> documentEntityDynamoDbAsyncTable;
    private final IndexingConfiguration indexingConfiguration;

    private final DocumentClientCall documentClientCall;
    private final UserConfigurationClientCall userConfigurationClientCall;
    private final RetryBackoffSpec gestoreRepositoryRetryStrategy;




    public AdditionalFileTagsServiceImpl(TagsClientCall tagsClientCall, ObjectMapper objectMapper, DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient, RepositoryManagerDynamoTableName repositoryManagerDynamoTableName, IndexingConfiguration indexingConfiguration, DocumentClientCall documentClientCall, UserConfigurationClientCall userConfigurationClientCall, RetryBackoffSpec gestoreRepositoryRetryStrategy) {
        this.tagsClientCall = tagsClientCall;
        this.objectMapper = objectMapper;
        this.indexingConfiguration = indexingConfiguration;
        this.documentClientCall = documentClientCall;
        this.userConfigurationClientCall = userConfigurationClientCall;
        this.gestoreRepositoryRetryStrategy = gestoreRepositoryRetryStrategy;
        this.tagsEntityDynamoDbAsyncTable = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedAsyncClient.table(repositoryManagerDynamoTableName.tagsName(), TableSchema.fromBean(TagsRelationsEntity.class)));
        this.documentEntityDynamoDbAsyncTable = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedAsyncClient.table(repositoryManagerDynamoTableName.documentiName(), TableSchema.fromBean(DocumentEntity.class)));
    }

    private Mono<? extends DocumentEntity> getErrorIdDocNotFoundException(String documentKey) {
        return Mono.error(new DocumentKeyNotPresentException(documentKey));
    }

    @Override
    public Mono<AdditionalFileTagsDto> getDocumentTags(String fileKey, String clientId) {
        final String GET_DOCUMENT = "AdditionalFileTagsService.getDocumentTags()";
        log.debug(LogUtils.INVOKING_METHOD, GET_DOCUMENT, fileKey);

        return userConfigurationClientCall.getUser(clientId)
                .retryWhen(gestoreRepositoryRetryStrategy)
                .flatMap(userConfigurationResponse -> {
                    boolean canReadTags = userConfigurationResponse.getUserConfiguration().getCanReadTags();

                    if (canReadTags) {
                        return documentClientCall.getDocument(fileKey)
                                .doOnError(DocumentKeyNotPresentException.class, throwable -> log.debug(throwable.getMessage()))
                                .flatMap(documentResponse -> {
                                    Map<String, List<String>> tags = documentResponse.getDocument().getTags();
                                    return removePrefixTags(tags);
                                })
                                .map(tags -> {
                                    AdditionalFileTagsDto additionalFileTagsDto = new AdditionalFileTagsDto();
                                    additionalFileTagsDto.setTags(tags);
                                    return additionalFileTagsDto;
                                });
                    } else {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                String.format("Client: %s does not have privilege to read tags of document %s", clientId, fileKey)));
                    }
                });
    }

    private Mono<Map<String, List<String>>> removePrefixTags(Map<String, List<String>> tags) {
        return Flux.fromIterable(tags.entrySet())
                .flatMap(entry -> {
                    String key = entry.getKey();
                    List<String> values = entry.getValue();
                    String[] parts = key.split("~", 2);

                    String tagKey = (parts.length == 2) ? parts[1] : key;
                    return Mono.just(Map.entry(tagKey, values));
                })
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }


}

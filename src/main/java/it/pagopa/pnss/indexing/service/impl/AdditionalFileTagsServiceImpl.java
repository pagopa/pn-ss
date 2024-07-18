package it.pagopa.pnss.indexing.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsDto;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsSearchResponseFileKeys;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.TagsClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.TagKeyValueNotPresentException;
import it.pagopa.pnss.common.exception.ClientNotAuthorizedException;
import it.pagopa.pnss.common.exception.InvalidSearchLogicException;
import it.pagopa.pnss.common.exception.RequestValidationException;
import it.pagopa.pnss.common.exception.IndexingLimitException;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configuration.IndexingConfiguration;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.indexing.service.AdditionalFileTagsService;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.repositorymanager.entity.TagsRelationsEntity;
import it.pagopa.pnss.repositorymanager.service.TagsService;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Override
    public Mono<AdditionalFileTagsUpdateResponse> postTags(String cxId, AdditionalFileTagsUpdateRequest request, String fileKey) {
        final String POST_TAG = "AdditionalFileTagsService.postTags()";
        log.debug(LogUtils.INVOKING_METHOD, POST_TAG, fileKey);

        return getPermission(cxId)
                .flatMap(authorizationGranted -> {
                    if (authorizationGranted) {
                        return documentClientCall.getDocument(fileKey)
                                .doOnError(DocumentKeyNotPresentException.class, throwable -> log.debug(throwable.getMessage()))
                                .flatMap(documentResponse ->
                                        requestValidation(request, cxId)
                                                .flatMap(tagsChanges ->
                                                        tagsClientCall.putTags(fileKey, tagsChanges)
                                                                .map(response -> {
                                                                    AdditionalFileTagsUpdateResponse updateResponse = new AdditionalFileTagsUpdateResponse();
                                                                    updateResponse.setResultCode("200.00");
                                                                    updateResponse.setResultDescription(response.toString());
                                                                    return updateResponse;
                                                                })
                                                )
                                );
                    } else {
                        return Mono.error(new ClientNotAuthorizedException(cxId));
                    }
                });
    }


    @Override
    public Mono<Boolean> getPermission(String cxId) {
        return userConfigurationClientCall.getUser(cxId).map(user -> user.getUserConfiguration().getCanWriteTags())
                .map(canWriteTags -> {
                    if (!canWriteTags) {
                        throw new ClientNotAuthorizedException(cxId);
                    }
                    return canWriteTags;
                });
    }

    @Override
    public Mono<TagsChanges> requestValidation(AdditionalFileTagsUpdateRequest request, String cxId) {
        return Mono.create(sink -> {
            TagsChanges tagsChanges = new TagsChanges();
            Map<String, List<String>> tagsToSet = new HashMap<>();
            Map<String, List<String>> tagsToDelete = new HashMap<>();

            try {
                Map<String, List<String>> setTags = request.getSET();
                Map<String, List<String>> deleteTags = request.getDELETE();

                if (setTags == null && deleteTags == null) {
                    throw new RequestValidationException("No tags to set nor delete.");
                }

                // I tag marcati con proprietà multivalue = false non possono avere più valori associati per la set
                if (setTags != null) {
                    for (Map.Entry<String, List<String>> entry : setTags.entrySet()) {
                        String tag = entry.getKey();
                        List<String> values = entry.getValue();

                        if (!indexingConfiguration.getTagInfo(tag).isMultivalue() && values.size() > 1) {
                            throw new RequestValidationException("Tag " + tag + " marked as singleValue cannot have multiple values");
                        }
                    }
                }

                // e per delete
                if (deleteTags != null) {
                    for (Map.Entry<String, List<String>> entry : deleteTags.entrySet()) {
                        String tag = entry.getKey();
                        List<String> values = entry.getValue();

                        if (!indexingConfiguration.getTagInfo(tag).isMultivalue() && values.size() > 1) {
                            throw new RequestValidationException("Tag " + tag + " marked as singleValue cannot have multiple values");
                        }
                    }
                }

                // set e delete non devono essere su stesso tag
                if (setTags != null && deleteTags != null) {
                    Set<String> commonTags = new HashSet<>(setTags.keySet());
                    commonTags.retainAll(deleteTags.keySet());
                    if (!commonTags.isEmpty()) {
                        throw new RequestValidationException("SET and DELETE cannot contain the same tags: " + commonTags);
                    }
                }

                // Il numero di tag da aggiornare deve essere <= maxTags
                if (setTags != null && deleteTags != null) {
                    if ((setTags.size() + deleteTags.size()) > indexingConfiguration.getIndexingLimits().getMaxTagsPerDocument()) {
                        throw new RequestValidationException("Number of tags to update exceeds maxTags limit");
                    }
                } else if (setTags != null && deleteTags == null) {
                    if (setTags.size() > indexingConfiguration.getIndexingLimits().getMaxTagsPerDocument()) {
                        throw new RequestValidationException("Number of tags to update exceeds maxTags limit");
                    }
                } else if (deleteTags != null && setTags == null) {
                    if (deleteTags.size() > indexingConfiguration.getIndexingLimits().getMaxTagsPerDocument()) {
                        throw new RequestValidationException("Number of tags to update exceeds maxTags limit");
                    }
                }

                // Il numero di values inseribili per tag deve essere <= maxValues per la set
                if (setTags != null) {
                    for (Map.Entry<String, List<String>> entry : setTags.entrySet()) {
                        if (entry.getValue().size() > indexingConfiguration.getIndexingLimits().getMaxValuesPerTagDocument()) {
                            throw new RequestValidationException("Number of values for tag " + entry.getKey() + " exceeds maxValues limit");
                        }
                    }
                }

                // e per la delete
                if (deleteTags != null) {
                    for (Map.Entry<String, List<String>> entry : deleteTags.entrySet()) {
                        if (entry.getValue().size() > indexingConfiguration.getIndexingLimits().getMaxValuesPerTagDocument()) {
                            throw new RequestValidationException("Number of values for tag " + entry.getKey() + " exceeds maxValues limit");
                        }
                    }
                }

                // Il  tag deve essere esistente
                if (setTags != null) {
                    for (String tag : setTags.keySet()) {
                        if (!indexingConfiguration.isTagValid(tag)) {
                            if (!indexingConfiguration.isTagValid(cxId + "~" + tag)) {
                                throw new RequestValidationException("Tag " + tag + " does not exist");
                            }
                            tagsToSet.put(cxId + "~" + tag, setTags.get(tag));
                        }
                        tagsToSet.put(tag, setTags.get(tag));
                    }
                }

                if (deleteTags != null) {
                    for (String tag : deleteTags.keySet()) {
                        if (!indexingConfiguration.isTagValid(tag)) {
                            if (!indexingConfiguration.isTagValid(cxId + "~" + tag)) {
                                throw new RequestValidationException("Tag " + tag + " does not exist");
                            }
                            tagsToDelete.put(cxId + "~" + tag, deleteTags.get(tag));
                        }
                        tagsToDelete.put(tag, deleteTags.get(tag));
                    }
                }
                sink.success(tagsChanges.SET(tagsToSet).DELETE(tagsToDelete));
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    @Override
    public Mono<List<AdditionalFileTagsSearchResponseFileKeys>> searchTags(String xPagopaSafestorageCxId, String logic, Boolean tags, Map<String, String> queryParams) {
        var reducingFunction = getLogicFunction(logic);
        return userConfigurationClientCall.getUser(xPagopaSafestorageCxId).handle((userConfigurationResponse, sink) -> {
                    if (Boolean.FALSE.equals(userConfigurationResponse.getUserConfiguration().getCanReadTags())) {
                        sink.error(new ResponseStatusException(HttpStatus.FORBIDDEN, String.format("Client: %s does not have privilege to read tags", xPagopaSafestorageCxId)));
                    } else sink.complete();
                })
                .thenMany(validateQueryParams(queryParams))
                .flatMap(this::getFileKeysList)
                .map(HashSet::new)
                .reduce(reducingFunction)
                .flatMapMany(Flux::fromIterable)
                .map(fileKey -> new AdditionalFileTagsSearchResponseFileKeys().fileKey(fileKey))
                .flatMap(fileKey -> {
                    if (Boolean.TRUE.equals(tags)) {
                        return documentClientCall.getDocument(fileKey.getFileKey()).map(documentResponse -> {
                            fileKey.setTags(documentResponse.getDocument().getTags());
                            return fileKey;
                        });
                    } else return Mono.just(fileKey);
                })
                .collectList();
    }

    private BinaryOperator<HashSet<String>> getLogicFunction(String logic) {
        return switch (logic) {
            case "or" -> orLogicFunction();
            case "and" -> andLogicFunction();
            default -> throw new InvalidSearchLogicException(logic);
        };
    }

    private BinaryOperator<HashSet<String>> orLogicFunction() {
        return (set1, set2) -> {
            set1.addAll(set2);
            return set1;
        };
    }

    private BinaryOperator<HashSet<String>> andLogicFunction() {
        return (set1, set2) -> {
            HashSet<String> intersection = new HashSet<>(set1);
            intersection.retainAll(set2);
            return intersection;
        };
    }

    private Mono<List<String>> getFileKeysList(Map.Entry<String, String> mapEntry) {
        return Mono.just(mapEntry).flatMap(entry -> tagsClientCall.getTagsRelations(entry.getKey() + "~" + entry.getValue()))
                .doOnError(TagKeyValueNotPresentException.class, e -> log.debug("TagKeyValueNotPresentException: {}", e.getMessage()))
                .map(tagsResponse -> tagsResponse.getTagsRelationsDto().getFileKeys())
                .onErrorResume(TagKeyValueNotPresentException.class, e -> Mono.fromSupplier(ArrayList::new));
    }

    private Flux<Map.Entry<String, String>> validateQueryParams(Map<String, String> queryParams) {
        // Inizializzo un flusso con i queryParam, escludendo i parametri "tags" e "logic"
        Flux<Map.Entry<String, String>> queryParamsFlux = Flux.fromIterable(queryParams.entrySet()).filter(entry -> !entry.getKey().equals("tags") && !entry.getKey().equals("logic"));
        return queryParamsFlux
                .count()
                .handle((count, sink) -> {
                    if (count > indexingConfiguration.getIndexingLimits().getMaxMapValuesForSearch()) {
                        sink.error(new IndexingLimitException("MaxMapValuesForSearch", Math.toIntExact(count), indexingConfiguration.getIndexingLimits().getMaxMapValuesForSearch()));
                    } else sink.complete();
                })
                .thenMany(queryParamsFlux);
    }

}
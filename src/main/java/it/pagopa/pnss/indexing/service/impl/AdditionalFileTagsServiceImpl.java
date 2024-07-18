package it.pagopa.pnss.indexing.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.TagsClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.exception.ClientNotAuthorizedException;
import it.pagopa.pnss.common.exception.RequestValidationException;
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
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.*;

@Service
@CustomLog
public class AdditionalFileTagsServiceImpl implements AdditionalFileTagsService {

    private final TagsClientCall tagsClientCall;
    private final IndexingConfiguration indexingConfiguration;
    private final DocumentClientCall documentClientCall;
    private final UserConfigurationClientCall userConfigurationClientCall;
    private final RetryBackoffSpec gestoreRepositoryRetryStrategy;

    public AdditionalFileTagsServiceImpl(TagsClientCall tagsClientCall,
                                         IndexingConfiguration indexingConfiguration,
                                         DocumentClientCall documentClientCall,
                                         UserConfigurationClientCall userConfigurationClientCall,
                                         RetryBackoffSpec gestoreRepositoryRetryStrategy) {
        this.tagsClientCall = tagsClientCall;
        this.indexingConfiguration = indexingConfiguration;
        this.documentClientCall = documentClientCall;
        this.userConfigurationClientCall = userConfigurationClientCall;
        this.gestoreRepositoryRetryStrategy = gestoreRepositoryRetryStrategy;
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

                validateRequest(setTags, deleteTags);
                processTags(setTags, cxId, tagsToSet);
                processTags(deleteTags, cxId, tagsToDelete);

                sink.success(tagsChanges.SET(tagsToSet).DELETE(tagsToDelete));
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    private void validateRequest(Map<String, List<String>> setTags, Map<String, List<String>> deleteTags) throws RequestValidationException {
        if (setTags == null && deleteTags == null) {
            throw new RequestValidationException("No tags to set nor delete.");
        }

        validateSingleValueTags(setTags);
        validateSingleValueTags(deleteTags);

        validateNoCommonTags(setTags, deleteTags);
        validateTagsLimit(setTags, deleteTags);

        validateMaxValuesPerTag(setTags);
        validateMaxValuesPerTag(deleteTags);
    }

    private void validateSingleValueTags(Map<String, List<String>> tags) throws RequestValidationException {
        if (tags != null) {
            for (Map.Entry<String, List<String>> entry : tags.entrySet()) {
                String tag = entry.getKey();
                List<String> values = entry.getValue();

                if (!indexingConfiguration.getTagInfo(tag).isMultivalue() && values.size() > 1) {
                    throw new RequestValidationException("Tag " + tag + " marked as singleValue cannot have multiple values");
                }
            }
        }
    }

    private void validateNoCommonTags(Map<String, List<String>> setTags, Map<String, List<String>> deleteTags) throws RequestValidationException {
        if (setTags != null && deleteTags != null) {
            Set<String> commonTags = new HashSet<>(setTags.keySet());
            commonTags.retainAll(deleteTags.keySet());
            if (!commonTags.isEmpty()) {
                throw new RequestValidationException("SET and DELETE cannot contain the same tags: " + commonTags);
            }
        }
    }

    private void validateTagsLimit(Map<String, List<String>> setTags, Map<String, List<String>> deleteTags) throws RequestValidationException {
        int setSize = setTags != null ? setTags.size() : 0;
        int deleteSize = deleteTags != null ? deleteTags.size() : 0;
        int maxTags = Math.toIntExact(indexingConfiguration.getIndexingLimits().getMaxTagsPerDocument());

        if ((setSize + deleteSize) > maxTags) {
            throw new RequestValidationException("Number of tags to update exceeds maxTags limit");
        }
    }

    private void validateMaxValuesPerTag(Map<String, List<String>> tags) throws RequestValidationException {
        if (tags != null) {
            int maxValues = Math.toIntExact(indexingConfiguration.getIndexingLimits().getMaxValuesPerTagDocument());
            for (Map.Entry<String, List<String>> entry : tags.entrySet()) {
                if (entry.getValue().size() > maxValues) {
                    throw new RequestValidationException("Number of values for tag " + entry.getKey() + " exceeds maxValues limit");
                }
            }
        }
    }

    private void processTags(Map<String, List<String>> tags, String cxId, Map<String, List<String>> result) throws RequestValidationException {
        if (tags != null) {
            for (Map.Entry<String, List<String>> entry : tags.entrySet()) {
                String tag = entry.getKey();

                if (!indexingConfiguration.isTagValid(tag)) {
                    if (!indexingConfiguration.isTagValid(cxId + "~" + tag)) {
                        throw new RequestValidationException("Tag " + tag + " does not exist");
                    }
                    result.put(cxId + "~" + tag, entry.getValue());
                } else {
                    result.put(tag, entry.getValue());
                }
            }
        }
    }

}
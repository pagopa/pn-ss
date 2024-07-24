package it.pagopa.pnss.indexing.service.impl;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsDto;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsSearchResponseFileKeys;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.TagsClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.TagKeyValueNotPresentException;
import it.pagopa.pnss.common.exception.ClientNotAuthorizedException;
import it.pagopa.pnss.common.exception.EmptyIntersectionException;
import it.pagopa.pnss.common.exception.RequestValidationException;
import it.pagopa.pnss.common.exception.IndexingLimitException;
import it.pagopa.pnss.common.exception.*;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configuration.IndexingConfiguration;
import it.pagopa.pnss.indexing.model.SearchLogic;
import it.pagopa.pnss.indexing.service.AdditionalFileTagsService;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.utils.LogUtils.*;

import static it.pagopa.pnss.common.utils.LogUtils.INVOKING_METHOD;
import static it.pagopa.pnss.common.utils.LogUtils.MDC_CORR_ID_KEY;

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
        log.debug(INVOKING_METHOD, GET_DOCUMENT, fileKey);

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
        return Mono.justOrEmpty(tags)
                .defaultIfEmpty(new HashMap<>())
                .flatMapMany(tagsMap -> Flux.fromIterable(tagsMap.entrySet()))
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
        log.debug(INVOKING_METHOD, POST_TAG, fileKey);

        return getWriteTagsPermission(cxId)
                .flatMap(authorizationGranted -> {
                    if (authorizationGranted) {
                        return postSingleTag(cxId, request, fileKey);
                    } else {
                        return Mono.error(new ClientNotAuthorizedException(cxId));
                    }
                });
    }

    private Mono<AdditionalFileTagsUpdateResponse> postSingleTag(String cxId, AdditionalFileTagsUpdateRequest request, String fileKey) {
        return requestValidation(request, cxId)
                .flatMap(tagsChanges ->
                        tagsClientCall.putTags(fileKey, tagsChanges)
                                .map(response -> {
                                    AdditionalFileTagsUpdateResponse updateResponse = new AdditionalFileTagsUpdateResponse();
                                    updateResponse.setResultCode("200.00");
                                    updateResponse.setResultDescription(response.toString());
                                    return updateResponse;
                                })
                );
    }

    private Mono<AdditionalFileTagsUpdateResponse> createUpdateResponse (Throwable throwable) {
        final String ERROR_400 = "400.00";
        if (throwable instanceof RequestValidationException) {
            return Mono.just(new AdditionalFileTagsUpdateResponse().resultCode(ERROR_400).resultDescription(throwable.getMessage()));
        }
        if (throwable instanceof MissingTagException) {
            return Mono.just (new AdditionalFileTagsUpdateResponse().resultCode(ERROR_400).resultDescription(throwable.getMessage()));
        }
        if (throwable instanceof PutTagsBadRequestException) {
            return Mono.just(new AdditionalFileTagsUpdateResponse().resultCode(ERROR_400).resultDescription(throwable.getMessage()));
        }
        return Mono.just ( new AdditionalFileTagsUpdateResponse().resultCode("500.00").resultDescription(throwable.getMessage()));
    }


    @Override
    public Mono<Boolean> getWriteTagsPermission(String cxId) {
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

    @Override
    public Mono<AdditionalFileTagsMassiveUpdateResponse> postMassiveTags(AdditionalFileTagsMassiveUpdateRequest request, String cxId) {
        final String POST_MASSIVE_TAG = "AdditionalFileTagsService.postMassiveTags()";
        log.debug(LogUtils.INVOKING_METHOD, POST_MASSIVE_TAG);

        return getWriteTagsPermission(cxId)
                .flatMap(authorizationGranted -> {
                    if (authorizationGranted) {
                        return handleMassiveUpdate(request, cxId);
                    } else {
                        return Mono.error(new ClientNotAuthorizedException(cxId));
                    }
                });
    }

    private Mono<AdditionalFileTagsMassiveUpdateResponse> handleMassiveUpdate(AdditionalFileTagsMassiveUpdateRequest request, String cxId) {
        return Mono.fromSupplier(() -> validateMassiveRequest(request.getTags()))
                .flatMapMany(Flux::fromIterable)
                .flatMap(tag -> processSingleRequest(cxId, tag.getSET(), tag.getDELETE(), tag.getFileKey()))
                .filter(this::hasError)
                .reduce(new HashMap<String, ErrorDetail>(), this::accumulateErrors)
                .flatMap(this::createResponse);
    }

    private Mono<ErrorDetail> processSingleRequest(String cxId, Map<String, List<String>> toSet, Map<String, List<String>> toDelete, String fileKey) {
        MDC.put(MDC_CORR_ID_KEY, fileKey);

        final String PROCESS_SINGLE_REQUEST = "AdditionalFileTagsService.processSingleRequest()";
        log.debug(INVOKING_METHOD, PROCESS_SINGLE_REQUEST);
        AdditionalFileTagsUpdateRequest singleRequest = new AdditionalFileTagsUpdateRequest()
                .SET(toSet)
                .DELETE(toDelete);

        return MDCUtils.addMDCToContextAndExecute(postSingleTag(cxId, singleRequest, fileKey)
                .map(response -> new ErrorDetail())
                .onErrorResume(throwable -> createUpdateResponse(throwable)
                        .map(error -> createErrorDetail(fileKey, throwable, error.getResultCode()))
                ));
    }

    private ErrorDetail createErrorDetail(String fileKey, Throwable throwable, String errorCode) {
        return new ErrorDetail()
                .fileKey(List.of(fileKey))
                .resultDescription(throwable.getMessage())
                .resultCode(errorCode);
    }

    private boolean hasError(ErrorDetail obj) {
        return obj.getResultDescription() != null;
    }

    private Map<String, ErrorDetail> accumulateErrors(Map<String, ErrorDetail> map, ErrorDetail err) {
        if (map.containsKey(err.getResultDescription())) {
            ErrorDetail existingErrDetail = map.get(err.getResultDescription());
            existingErrDetail.getFileKey().addAll(err.getFileKey());
            map.put(err.getResultDescription(), existingErrDetail);
        } else {
            map.put(err.getResultDescription(), err);
        }
        return map;
    }

    private Mono<AdditionalFileTagsMassiveUpdateResponse> createResponse(Map<String, ErrorDetail> errorMap) {
        List<ErrorDetail> errList = new ArrayList<>(errorMap.values());
        return Mono.just(new AdditionalFileTagsMassiveUpdateResponse().errors(errList));
    }

    private List<Tags> validateMassiveRequest(List<Tags> tagList) throws RequestValidationException {
        List<Tags> tagsList = tagList;
        if (tagsList.isEmpty()) {
            throw new RequestValidationException("No tags to set nor delete.");
        }

        if (tagsList.size() > indexingConfiguration.getIndexingLimits().getMaxFileKeysUpdateMassivePerRequest()) {
            throw new RequestValidationException("Number of documents to update exceeds MaxFileKeysUpdateMassivePerRequest limit.");
        }

        Map<String, Tags> tagsMap = new HashMap<>();
        for (Tags tag : tagList) {
            String fileKey = tag.getFileKey();
            if (tagsMap.containsKey(fileKey)) {
                throw new RequestValidationException("Duplicate fileKey found: " + fileKey);
            }
            tagsMap.put(fileKey, tag);
        }
        return tagsList;
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
        int maxTags = Math.toIntExact(indexingConfiguration.getIndexingLimits().getMaxOperationsOnTagsPerRequest());

        if ((setSize + deleteSize) > maxTags) {
            throw new RequestValidationException("Number of tags to update exceeds maxOperationsOnTags limit");
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
                        throw new RequestValidationException("Tag " + tag + " not found in the indexing configuration");
                    }
                    result.put(cxId + "~" + tag, entry.getValue());
                } else {
                    result.put(tag, entry.getValue());
                }
            }
        }
    }

    /**
     * A method to search for fileKeys having the given tags with the given values passed as query parameters.
     *
     * @param xPagopaSafestorageCxId the clientId
     * @param logic the search logic to apply
     * @param tags the option to retrieve fileKeys with or without tags
     * @param queryParams the query parameters
     * @return Mono<List<AdditionalFileTagsSearchResponseFileKeys>> the mono containing the list of fileKeys with or without tags
     */
    @Override
    public Mono<List<AdditionalFileTagsSearchResponseFileKeys>> searchTags(String xPagopaSafestorageCxId, String logic, Boolean tags, Map<String, String> queryParams) {
        log.debug(INVOKING_METHOD, SEARCH_TAGS, Stream.of(xPagopaSafestorageCxId, logic, tags, queryParams).toList());
        var reducingFunction = getLogicFunction(logic);
        return userConfigurationClientCall.getUser(xPagopaSafestorageCxId).handle((userConfigurationResponse, sink) -> {
                    if (Boolean.FALSE.equals(userConfigurationResponse.getUserConfiguration().getCanReadTags())) {
                        sink.error(new ResponseStatusException(HttpStatus.FORBIDDEN, String.format("Client: %s does not have privilege to read tags", xPagopaSafestorageCxId)));
                    } else sink.complete();
                })
                .thenMany(Flux.defer(() -> validateQueryParams(queryParams)))
                .flatMap(this::getFileKeysList)
                .map(HashSet::new)
                .reduce(reducingFunction)
                .onErrorResume(EmptyIntersectionException.class, throwable -> Mono.just(new HashSet<>()))
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
                .collectList()
                .doOnSuccess(result -> log.debug(SUCCESSFUL_OPERATION_LABEL, SEARCH_TAGS, result));
    }

    /**
     * A method to get the right function to apply based on logic parameter.
     *
     * @param logic the search logic to apply
     * @return BinaryOperator<HashSet<String>> a binary operator
     */
    private BinaryOperator<HashSet<String>> getLogicFunction(String logic) {
        return switch (SearchLogic.getEnumLogic(logic)) {
            case OR -> orLogicFunction();
            case AND -> andLogicFunction();
        };
    }

    /**
     * The function to apply in case of OR logic
     *
     * @return BinaryOperator<HashSet<String>> a binary operator
     */
    private BinaryOperator<HashSet<String>> orLogicFunction() {
        return (set1, set2) -> {
            set1.addAll(set2);
            return set1;
        };
    }

    /**
     * The function to apply in case of AND logic
     *
     * @return BinaryOperator<HashSet < String>> a binary operator
     */
    private BinaryOperator<HashSet<String>> andLogicFunction() {
        return (set1, set2) -> {
            HashSet<String> intersection = new HashSet<>(set1);
            intersection.retainAll(set2);
            // we throw an exception to stop the reducing process when the intersection is empty.
            if (intersection.isEmpty()) {
                throw new EmptyIntersectionException();
            }
            return intersection;
        };
    }

    /**
     * A method to get the list of fileKeys with the given tag and value.
     *
     * @param mapEntry the tag and value to search
     * @return Mono<List<String>> the mono containing the list of fileKeys
     */
    private Mono<List<String>> getFileKeysList(Map.Entry<String, String> mapEntry) {
        return Mono.just(mapEntry).flatMap(entry -> tagsClientCall.getTagsRelations(entry.getKey() + "~" + entry.getValue()))
                .map(tagsResponse -> tagsResponse.getTagsRelationsDto().getFileKeys())
                .onErrorResume(TagKeyValueNotPresentException.class, e -> Mono.fromSupplier(ArrayList::new));
    }

    /**
     * A method to validate the query parameters.
     * It checks if the number of tag key-value pairs exceeds the MaxMapValuesForSearch setting.
     *
     * @param queryParams the query parameters to check
     * @return Flux<Map.Entry<String, String>> the flux containing the query parameters, without the "tags" and "logic" parameters
     */
    private Flux<Map.Entry<String, String>> validateQueryParams(Map<String, String> queryParams) {
        log.debug(INVOKING_METHOD, VALIDATE_QUERY_PARAMS, queryParams);
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
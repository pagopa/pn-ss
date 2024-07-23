package it.pagopa.pnss.indexing.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.AdditionalFileTagsApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsGetResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsSearchResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.common.exception.*;
import it.pagopa.pnss.indexing.model.SearchLogic;
import it.pagopa.pnss.indexing.service.AdditionalFileTagsService;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsUpdateRequest;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsUpdateResponse;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static it.pagopa.pnss.common.utils.LogUtils.*;

@CustomLog
@RestController
public class AdditionalFileTagsController implements AdditionalFileTagsApi {
    private final AdditionalFileTagsService additionalFileTagsService;
    private static final String RESULT_CODE_400 = "400.00";

    public AdditionalFileTagsController(AdditionalFileTagsService additionalFileTagsService) {
        this.additionalFileTagsService = additionalFileTagsService;
    }

    @ExceptionHandler(MissingTagException.class)
    public ResponseEntity<AdditionalFileTagsUpdateResponse> handleMissingTagException(MissingTagException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AdditionalFileTagsUpdateResponse().resultCode(RESULT_CODE_400).resultDescription(ex.getMessage()));
    }

    @ExceptionHandler(DocumentKeyNotPresentException.class)
    public ResponseEntity<String> handleDocumentNotFoundException(DocumentKeyNotPresentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IdClientNotFoundException.class)
    public ResponseEntity<String> handleUnauthorizedException(IdClientNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(ClientNotAuthorizedException.class)
    public ResponseEntity<AdditionalFileTagsUpdateResponse> handleClientNotAuthorizedException(ClientNotAuthorizedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AdditionalFileTagsUpdateResponse().resultCode("403.00").resultDescription(ex.getMessage()));
    }

    @ExceptionHandler(RequestValidationException.class)
    public ResponseEntity<AdditionalFileTagsUpdateResponse> handleRequestValidationException(RequestValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AdditionalFileTagsUpdateResponse().resultCode(RESULT_CODE_400).resultDescription(ex.getMessage()));
    }

    @ExceptionHandler(IndexingLimitException.class)
    public ResponseEntity<String> handleIndexingLimitException(IndexingLimitException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(PutTagsBadRequestException.class)
    public ResponseEntity<AdditionalFileTagsUpdateResponse> handlePutTagsBadRequestException(PutTagsBadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AdditionalFileTagsUpdateResponse().resultCode(RESULT_CODE_400).resultDescription(ex.getMessage()));
    }

    @ExceptionHandler(InvalidSearchLogicException.class)
    public ResponseEntity<String> handleInvalidSearchLogicException(InvalidSearchLogicException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @Override
    public Mono<ResponseEntity<AdditionalFileTagsGetResponse>> additionalFileTagsGet(String fileKey, String xPagopaSafestorageCxId, final ServerWebExchange exchange) {
        log.logStartingProcess(GET_TAGS_DOCUMENT);
        return additionalFileTagsService.getDocumentTags(fileKey, xPagopaSafestorageCxId)
                .map(additionalFileTagsDto -> {
                    Map<String, List<String>> tags = additionalFileTagsDto.getTags();
                    AdditionalFileTagsGetResponse response = new AdditionalFileTagsGetResponse().tags(tags);
                    return ResponseEntity.ok().body(response);
                })
                .doOnSuccess(result -> log.logEndingProcess(GET_TAGS_DOCUMENT))
                .doOnError(throwable -> log.logEndingProcess(GET_TAGS_DOCUMENT, false, throwable.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<AdditionalFileTagsSearchResponse>> additionalFileTagsSearch(String xPagopaSafestorageCxId, String logic, Boolean tags, Map<String, String> tagParams, final ServerWebExchange exchange) {
        MDC.put(MDC_CORR_ID_KEY, UUID.randomUUID().toString());
        log.logStartingProcess(ADDITIONAL_FILE_TAGS_SEARCH);
        logic = logic == null ? SearchLogic.AND.getLogic() : logic;
        tags = tags != null && tags;
        return MDCUtils.addMDCToContextAndExecute(additionalFileTagsService.searchTags(xPagopaSafestorageCxId, logic, tags, tagParams)
                .map(fileKeys -> ResponseEntity.ok().body(new AdditionalFileTagsSearchResponse().fileKeys(fileKeys)))
                .doOnSuccess(result -> log.logEndingProcess(ADDITIONAL_FILE_TAGS_SEARCH))
                .doOnError(throwable -> log.logEndingProcess(ADDITIONAL_FILE_TAGS_SEARCH, false, throwable.getMessage())));
    }

    @Override
    public Mono<ResponseEntity<AdditionalFileTagsMassiveUpdateResponse>> additionalFileTagsMassiveUpdate(String xPagopaSafestorageCxId,
                                                                                                         Mono<AdditionalFileTagsMassiveUpdateRequest> additionalFileTagsMassiveUpdateRequest,
                                                                                                         final ServerWebExchange exchange) {
        log.logStartingProcess(MASSIVE_POST_TAG_DOCUMENT);
        return additionalFileTagsMassiveUpdateRequest.flatMap(request -> additionalFileTagsService.postMassiveTags(request, xPagopaSafestorageCxId).map(response -> {
                    return ResponseEntity.ok().body(response);
                })).doOnSuccess(result -> log.logEndingProcess(MASSIVE_POST_TAG_DOCUMENT))
                .doOnError(throwable -> log.logEndingProcess(MASSIVE_POST_TAG_DOCUMENT, false, throwable.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<AdditionalFileTagsUpdateResponse>> additionalFileTagsUpdate(String fileKey, String xPagopaSafestorageCxId,
                                                                                           Mono<AdditionalFileTagsUpdateRequest> additionalFileTagsUpdateRequest,
                                                                                           final ServerWebExchange exchange) {
        log.logStartingProcess(POST_TAG_DOCUMENT);
        return additionalFileTagsUpdateRequest.flatMap(request -> additionalFileTagsService.postTags(xPagopaSafestorageCxId, request, fileKey)
                        .map(response -> ResponseEntity.ok().body(response))).doOnSuccess(result -> log.logEndingProcess(POST_TAG_DOCUMENT))
                .onErrorResume(DocumentKeyNotPresentException.class, throwable -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new AdditionalFileTagsUpdateResponse().resultCode("404.00").resultDescription(throwable.getMessage()))))
                .doOnError(throwable -> log.logEndingProcess(POST_TAG_DOCUMENT, false, throwable.getMessage()));
    }

}

package it.pagopa.pnss.indexing.rest;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.AdditionalFileTagsApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsGetResponse;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.indexing.service.AdditionalFileTagsService;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.AdditionalFileTagsApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsUpdateRequest;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsUpdateResponse;
import lombok.CustomLog;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static it.pagopa.pnss.common.utils.LogUtils.GET_TAGS_DOCUMENT;

@CustomLog
@RestController
public class AdditionalFileTagsController implements AdditionalFileTagsApi {
    private final AdditionalFileTagsService additionalFileTagsService;

    public AdditionalFileTagsController(AdditionalFileTagsService additionalFileTagsService) {
        this.additionalFileTagsService = additionalFileTagsService;
    }

    @ExceptionHandler(DocumentKeyNotPresentException.class)
    public ResponseEntity<String> handleDocumentNotFoundException(DocumentKeyNotPresentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
    @ExceptionHandler(IdClientNotFoundException.class)
    public ResponseEntity<String> handleUnauthorizedException(IdClientNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
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



public class AdditionalFileTagsController implements AdditionalFileTagsApi {

    @Override
    public Mono<ResponseEntity<AdditionalFileTagsUpdateResponse>> additionalFileTagsUpdate(String fileKey, Mono<AdditionalFileTagsUpdateRequest> additionalFileTagsUpdateRequest, ServerWebExchange exchange) {
        return AdditionalFileTagsApi.super.additionalFileTagsUpdate(fileKey, additionalFileTagsUpdateRequest, exchange);
    }
}

package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.TagsInternalApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Error;
import it.pagopa.pnss.repositorymanager.exception.IndexingLimitException;
import it.pagopa.pnss.repositorymanager.service.TagsService;
import lombok.CustomLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pnss.common.utils.LogUtils.GET_TAGS;
import static it.pagopa.pnss.common.utils.LogUtils.PUT_TAGS;

@CustomLog
@RestController
public class TagsInternalApiController implements TagsInternalApi {
    private final TagsService tagsService;

    public TagsInternalApiController(TagsService tagsService) {
        this.tagsService = tagsService;
    }

    @Override
    public Mono<ResponseEntity<TagsResponse>> getTags(String tagKeyValue, ServerWebExchange exchange) {
        return tagsService.getTags(tagKeyValue).map(tagsDto -> ResponseEntity.ok(new TagsResponse().tagsDto(tagsDto)))
                .doOnSuccess(result -> log.logEndingProcess(GET_TAGS))
                .doOnError(throwable -> log.logEndingProcess(GET_TAGS, false, throwable.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<TagsUpdateResponse>> putTags(Mono<TagsChanges> tagsChanges, ServerWebExchange exchange) {
        return tagsChanges.flatMap(tagsService::updateTags).map(result -> ResponseEntity.ok().body(new TagsUpdateResponse().tags(result)))
                .doOnSuccess(result -> log.logEndingProcess(PUT_TAGS))
                .onErrorResume(IndexingLimitException.class, throwable -> Mono.just(ResponseEntity
                        .badRequest()
                        .body(new TagsUpdateResponse().error(new Error().code("400").description(throwable.getMessage())))))
                .doOnError(throwable -> log.logEndingProcess(PUT_TAGS, false, throwable.getMessage()));
    }
}

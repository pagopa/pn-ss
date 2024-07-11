package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.TagsInternalApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.repositorymanager.service.TagsService;
import lombok.CustomLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pnss.common.utils.LogUtils.GET_TAGS_RELATIONS_OP;
import static it.pagopa.pnss.common.utils.LogUtils.PUT_TAGS_OP;

@CustomLog
@RestController
public class TagsInternalApiController implements TagsInternalApi {
    private final TagsService tagsService;

    public TagsInternalApiController(TagsService tagsService) {
        this.tagsService = tagsService;
    }

    @Override
    public Mono<ResponseEntity<TagsRelationsResponse>> getTagsRelations(String tagKeyValue, final ServerWebExchange exchange) {
        log.logStartingProcess(GET_TAGS_RELATIONS_OP);
        return tagsService.getTagsRelations(tagKeyValue)
                .map(tagOutput -> ResponseEntity.ok().body(new TagsRelationsResponse().tagsRelationsDto(tagOutput)))
                .doOnSuccess(result -> log.logEndingProcess(GET_TAGS_RELATIONS_OP))
                .doOnError(throwable -> log.logEndingProcess(GET_TAGS_RELATIONS_OP, false, throwable.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<TagsResponse>> putTags(String documentKey, Mono<TagsChanges> tagsChanges, ServerWebExchange exchange) {
        return tagsChanges.flatMap(changes -> tagsService.updateTags(documentKey, changes))
                .map(tagsDto -> ResponseEntity.ok().body(new TagsResponse().tagsDto(tagsDto)))
                .doOnSuccess(result -> log.logEndingProcess(PUT_TAGS_OP))
                .doOnError(throwable -> log.logEndingProcess(PUT_TAGS_OP, false, throwable.getMessage()));
    }
}

package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.TagsInternalApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Error;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.common.client.exception.RetentionException;
import it.pagopa.pnss.common.exception.InvalidNextStatusException;
import it.pagopa.pnss.repositorymanager.exception.IllegalDocumentStateException;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.exception.TagKeyNotPresentException;
import it.pagopa.pnss.repositorymanager.service.TagsService;
import lombok.CustomLog;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.time.DateTimeException;

import static it.pagopa.pnss.common.utils.LogUtils.GET_TAGS;
import static it.pagopa.pnss.common.utils.LogUtils.PUT_TAGS;

@CustomLog
@RestController
public class TagsInternalApiController implements TagsInternalApi {
    private final TagsService tagsService;

    public TagsInternalApiController(TagsService tagsService) {
        this.tagsService = tagsService;
    }

    private TagsResponse getResponse(TagsDto tagsDto) {
        TagsResponse response = new TagsResponse();
        response.setTagsDto(tagsDto);
        return response;
    }
    private Mono<ResponseEntity<TagsResponse>> buildErrorResponse(HttpStatus httpStatus, String errorMsg) {
        TagsResponse response = new TagsResponse();
        Error error=new Error();
        error.setCode(httpStatus.name());
        response.setError(error);
        response.getError().setDescription(errorMsg);
        return Mono.just(ResponseEntity.status(httpStatus).body(response));
    }

    private Mono<ResponseEntity<TagsResponse>> buildErrorResponse(HttpStatus httpStatus, Throwable throwable) {
        TagsResponse response = new TagsResponse();
        Error error=new Error();
        error.setCode(httpStatus.name());
        response.setError(error);
        response.getError().setDescription(throwable.getMessage());
        return Mono.just(ResponseEntity.status(httpStatus).body(response));
    }
    private Mono<ResponseEntity<TagsResponse>> getResponse(String tagKeyValue, Throwable throwable) {
        TagsResponse response = new TagsResponse();
        response.setError(new Error());

        response.getError().setDescription(throwable.getMessage());

        if (throwable instanceof ItemAlreadyPresent) {
            String errorMsg = tagKeyValue == null ? "Tag already present"
                    : String.format("Tag with id %s already present", tagKeyValue);
            return buildErrorResponse(HttpStatus.CONFLICT, errorMsg);
        } else if (throwable instanceof TagKeyNotPresentException) {
            String errorMsg = tagKeyValue == null ? "Tag not found"
                    : String.format("Tag with id %s not found", tagKeyValue);
            return buildErrorResponse(HttpStatus.NOT_FOUND, errorMsg);
        } else if (throwable instanceof RepositoryManagerException) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, throwable);
        } else if (throwable instanceof NoSuchKeyException) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, throwable);
        } else {
            log.error("Internal Error ---> {}", throwable.getMessage());
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, throwable);
        }
    }



    @Override
    public Mono<ResponseEntity<TagsResponse>> getTags(String tagKeyValue, final ServerWebExchange exchange) {
        log.logStartingProcess(GET_TAGS);
        log.debug("Request headers for '{}' : {}", GET_TAGS, exchange.getRequest().getHeaders());
        return MDCUtils.addMDCToContextAndExecute(tagsService.getTags(tagKeyValue)
                .map(tagOutput -> ResponseEntity.ok(getResponse(tagOutput)))
                .doOnSuccess(result -> log.logEndingProcess(GET_TAGS))
                .onErrorResume(throwable -> {
                    log.logEndingProcess(GET_TAGS, false, throwable.getMessage());
                    return getResponse(tagKeyValue, throwable);
                }));
    }

    @Override
    public Mono<ResponseEntity<TagsUpdateResponse>> putTags(Mono<TagsChanges> tagsChanges, ServerWebExchange exchange) {
        return tagsChanges.flatMap(tagsService::updateTags).map(result -> ResponseEntity.ok().body(new TagsUpdateResponse().tags(result)))
                .doOnSuccess(result -> log.logEndingProcess(PUT_TAGS))
                .doOnError(throwable -> log.logEndingProcess(PUT_TAGS, false, throwable.getMessage()));
    }
}

package it.pagopa.pnss.common.client.impl;


import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsRelationsResponse;
import it.pagopa.pnss.common.client.TagsClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.TagKeyValueNotPresentException;
import it.pagopa.pnss.common.exception.PutTagsBadRequestException;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static it.pagopa.pnss.common.utils.LogUtils.INVOKING_INTERNAL_SERVICE;
import static it.pagopa.pnss.common.utils.LogUtils.REPOSITORY_MANAGER;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@CustomLog
@Service
public class TagsClientCallImpl implements TagsClientCall {

    @Value("${header.x-pagopa-safestorage-cx-id}")
    private String xPagopaSafestorageCxId;

    @Value("${gestore.repository.anagrafica.internal.tags.get}")
    private String anagraficaTagsClientEndpointGet;

    @Value("${gestore.repository.anagrafica.internal.tags.put}")
    private String anagraficaTagsClientEndpointPut;

    private final WebClient ssWebClient;

    public TagsClientCallImpl(WebClient ssWebClient) {
        this.ssWebClient = ssWebClient;
    }

    @Override
    public Mono<TagsRelationsResponse> getTagsRelations(String tagKeyValue) {
        log.info(INVOKING_INTERNAL_SERVICE, REPOSITORY_MANAGER, "getTagsRelations()");
        return ssWebClient.get()
                .uri(String.format(anagraficaTagsClientEndpointGet, tagKeyValue))
                .retrieve()
                .onStatus(NOT_FOUND::equals, clientResponse -> Mono.error(new TagKeyValueNotPresentException(tagKeyValue)))
                .bodyToMono(TagsRelationsResponse.class);
    }

    @Override
    public Mono<TagsResponse> putTags(String documentKey, TagsChanges tagsChanges) {
        log.info(INVOKING_INTERNAL_SERVICE, REPOSITORY_MANAGER, "putTags()");
        return ssWebClient.put()
                .uri(String.format(anagraficaTagsClientEndpointPut, documentKey))
                .bodyValue(tagsChanges)
                .retrieve()
                .onStatus(BAD_REQUEST::equals, this::createPutTagsBadRequestException)
                .onStatus(NOT_FOUND::equals, clientResponse -> Mono.error(new DocumentKeyNotPresentException(documentKey)))
                .bodyToMono(TagsResponse.class);
    }

    private Mono<? extends Throwable> createPutTagsBadRequestException(ClientResponse clientResponse) {
        return clientResponse
                .bodyToMono(TagsResponse.class)
                .filter(tagsResponse -> tagsResponse.getError() != null || tagsResponse.getError().getDescription() != null)
                .switchIfEmpty(Mono.error(new PutTagsBadRequestException()))
                .flatMap(response -> Mono.error(new PutTagsBadRequestException(response.getError().getDescription())));
    }

}

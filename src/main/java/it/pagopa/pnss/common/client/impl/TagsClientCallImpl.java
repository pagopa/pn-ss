package it.pagopa.pnss.common.client.impl;


import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsRelationsResponse;
import it.pagopa.pnss.common.client.TagsClientCall;
import it.pagopa.pnss.common.client.exception.TagKeyValueNotPresentException;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static it.pagopa.pnss.common.utils.LogUtils.INVOKING_INTERNAL_SERVICE;
import static it.pagopa.pnss.common.utils.LogUtils.REPOSITORY_MANAGER;
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
                .uri( builder -> builder.path(anagraficaTagsClientEndpointPut)
                        .build(documentKey))
                .bodyValue(tagsChanges)
                .retrieve()
                .bodyToMono(TagsResponse.class);
    }

}

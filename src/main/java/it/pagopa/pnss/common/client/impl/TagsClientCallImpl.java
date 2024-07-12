package it.pagopa.pnss.common.client.impl;


import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsResponse;
import it.pagopa.pnss.common.client.TagsClientCall;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@CustomLog
@Service
public class TagsClientCallImpl implements TagsClientCall {

    @Value("${gestore.repository.anagrafica.internal.tags.get}")
    private String anagraficaTagsClientEndpointGet;

    @Value("${gestore.repository.anagrafica.internal.tags.put}")
    private String anagraficaTagsClientEndpointPut;

    private final WebClient ssWebClient;

    public TagsClientCallImpl(WebClient ssWebClient) {
        this.ssWebClient = ssWebClient;
    }

    @Override
    public Mono<TagsResponse> putTags(String documentKey, TagsChanges tagsChanges) {
        return ssWebClient.put()
                          .uri( builder -> builder.path(anagraficaTagsClientEndpointPut)
                          .build(documentKey))
                          .bodyValue(tagsChanges)
                          .retrieve()
                          .bodyToMono(TagsResponse.class);
    }

}

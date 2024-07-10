package it.pagopa.pnss.common.client.impl;


import it.pagopa.pnss.common.client.TagsClientCall;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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


}

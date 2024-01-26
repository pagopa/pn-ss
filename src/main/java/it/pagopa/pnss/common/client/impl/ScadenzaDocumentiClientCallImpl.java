package it.pagopa.pnss.common.client.impl;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.ScadenzaDocumentiInput;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.ScadenzaDocumentiResponse;
import it.pagopa.pnss.common.client.ScadenzaDocumentiClientCall;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@CustomLog
public class ScadenzaDocumentiClientCallImpl implements ScadenzaDocumentiClientCall {

    @Autowired
    private WebClient ssWebClient;
    @Value("${gestore.repository.anagrafica.internal.scadenza.documenti.post}")
    private String scadenzaDocumentiEndpointPost;

    @Override
    public Mono<ScadenzaDocumentiResponse> insertOrUpdateScadenzaDocumenti(ScadenzaDocumentiInput scadenzaDocumentiInput) {
        return ssWebClient.post()
                .uri(scadenzaDocumentiEndpointPost)
                .bodyValue(scadenzaDocumentiInput)
                .retrieve()
                .bodyToMono(ScadenzaDocumentiResponse.class);
    }
}
package it.pagopa.pnss.common.client.impl;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Error;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.ScadenzaDocumentiInput;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.ScadenzaDocumentiResponse;
import it.pagopa.pnss.common.client.ScadenzaDocumentiClientCall;
import it.pagopa.pnss.common.client.exception.ScadenzaDocumentiCallException;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@CustomLog
public class ScadenzaDocumentiClientCallImpl implements ScadenzaDocumentiClientCall {

    private final WebClient ssWebClient;
    @Value("${gestore.repository.anagrafica.internal.scadenza.documenti.post}")
    private String scadenzaDocumentiEndpointPost;

    public ScadenzaDocumentiClientCallImpl(WebClient ssWebClient) {
        this.ssWebClient = ssWebClient;
    }

    @Override
    public Mono<ScadenzaDocumentiResponse> insertOrUpdateScadenzaDocumenti(ScadenzaDocumentiInput scadenzaDocumentiInput) {
        return ssWebClient.post()
                .uri(scadenzaDocumentiEndpointPost)
                .bodyValue(scadenzaDocumentiInput)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(ScadenzaDocumentiResponse.class)
                        .flatMap(scadenzaDocumentiResponse -> {
                            Error error = scadenzaDocumentiResponse.getError();
                            if (error != null) {
                                return Mono.error(new ScadenzaDocumentiCallException(Integer.parseInt(error.getCode()), error.getDescription()));
                            } else return clientResponse.createException();
                        }))
                .bodyToMono(ScadenzaDocumentiResponse.class);
    }
}
package it.pagopa.pnss.common.client.impl;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.ScadenzaDocumentiInput;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.ScadenzaDocumentiResponse;
import it.pagopa.pnss.common.client.ScadenzaDocumentiClientCall;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@CustomLog
public class ScadenzaDocumentiClientCallImpl implements ScadenzaDocumentiClientCall {
    @Override
    public Mono<ScadenzaDocumentiResponse> insertOrUpdateScadenzaDocumenti(ScadenzaDocumentiInput scadenzaDocumentiInput) {
        return null;
    }
}
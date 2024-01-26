package it.pagopa.pnss.common.client;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.ScadenzaDocumentiInput;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.ScadenzaDocumentiResponse;
import reactor.core.publisher.Mono;


public interface ScadenzaDocumentiClientCall {

    Mono<ScadenzaDocumentiResponse> insertOrUpdateScadenzaDocumenti(ScadenzaDocumentiInput scadenzaDocumentiInput);

}

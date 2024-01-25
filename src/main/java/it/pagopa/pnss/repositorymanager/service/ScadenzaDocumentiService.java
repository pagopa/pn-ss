package it.pagopa.pnss.repositorymanager.service;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.ScadenzaDocumenti;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.ScadenzaDocumentiInput;
import reactor.core.publisher.Mono;

public interface ScadenzaDocumentiService {

    Mono<ScadenzaDocumenti> insertOrUpdateScadenzaDocumenti(ScadenzaDocumentiInput scadenzaDocumentiInput);

}

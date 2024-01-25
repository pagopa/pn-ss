package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.ScadenzaDocumentiInternalApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.ScadenzaDocumentiInput;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.ScadenzaDocumentiResponse;
import it.pagopa.pnss.repositorymanager.service.ScadenzaDocumentiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class ScadenzaDocumentiInternalApiController implements ScadenzaDocumentiInternalApi {

    @Autowired
    private ScadenzaDocumentiService scadenzaDocumentiService;

    @Override
    public Mono<ResponseEntity<ScadenzaDocumentiResponse>> insertOrUpdateScadenzaDocumenti(Mono<ScadenzaDocumentiInput> scadenzaDocumentiInput, ServerWebExchange exchange) {
        return scadenzaDocumentiInput.flatMap(scadenzaDocumenti -> scadenzaDocumentiService.insertOrUpdateScadenzaDocumenti(scadenzaDocumenti))
                .map(scadenzaDocumenti -> new ScadenzaDocumentiResponse().document(scadenzaDocumenti))
                .map(ResponseEntity::ok);
    }

}

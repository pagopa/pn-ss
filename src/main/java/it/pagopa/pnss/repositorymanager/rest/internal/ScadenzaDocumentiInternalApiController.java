package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.ScadenzaDocumentiInternalApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Error;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.ScadenzaDocumentiInput;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.ScadenzaDocumentiResponse;
import it.pagopa.pnss.common.exception.IdemPotentElementException;
import it.pagopa.pnss.common.utils.LogUtils;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.ScadenzaDocumentiService;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@CustomLog
public class ScadenzaDocumentiInternalApiController implements ScadenzaDocumentiInternalApi {

    @Autowired
    private ScadenzaDocumentiService scadenzaDocumentiService;

    @Override
    public Mono<ResponseEntity<ScadenzaDocumentiResponse>> insertOrUpdateScadenzaDocumenti(Mono<ScadenzaDocumentiInput> scadenzaDocumentiInput, ServerWebExchange exchange) {
        log.logStartingProcess(LogUtils.INSERT_OR_UPDATE_SCADENZA_DOCUMENTI);
        return scadenzaDocumentiInput.flatMap(scadenzaDocumenti -> scadenzaDocumentiService.insertOrUpdateScadenzaDocumenti(scadenzaDocumenti))
                .map(scadenzaDocumenti -> new ScadenzaDocumentiResponse().document(scadenzaDocumenti))
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> log.logEndingProcess(LogUtils.INSERT_OR_UPDATE_SCADENZA_DOCUMENTI))
                .doOnError(throwable -> log.logEndingProcess(LogUtils.INSERT_OR_UPDATE_SCADENZA_DOCUMENTI, false, throwable.getMessage()))
                .onErrorResume(RepositoryManagerException.class, e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ScadenzaDocumentiResponse().error(
                        new Error().code("400").description(e.getMessage())))))
                .onErrorResume(DynamoDbException.class, e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ScadenzaDocumentiResponse().error(
                        new Error().code(String.valueOf(e.statusCode())).description(e.getMessage())))));

    }

}

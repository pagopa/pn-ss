package it.pagopa.pnss.common.client.impl;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentTypeResponse;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static it.pagopa.pnss.common.utils.LogUtils.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@CustomLog
public class DocTypesClientCallImpl implements DocTypesClientCall {

    private final WebClient ssWebClient;

    @Value("${gestore.repository.anagrafica.internal.docTypes}")
    private String anagraficaDocTypesInternalClientEndpoint;

    public DocTypesClientCallImpl(WebClient ssWebClient) {
        this.ssWebClient = ssWebClient;
    }

    @Override
    public Mono<DocumentTypeResponse> getdocTypes(String tipologiaDocumento) throws IdClientNotFoundException {
        log.debug(INVOKING_INTERNAL_SERVICE, REPOSITORY_MANAGER, GET_DOC_TYPES);
        var mdcContextMap = MDCUtils.retrieveMDCContextMap();
        return ssWebClient.get()
                          .uri(String.format(anagraficaDocTypesInternalClientEndpoint, tipologiaDocumento))
                          .retrieve()
                          .onStatus(NOT_FOUND::equals, response -> Mono.error(new DocumentTypeNotPresentException(tipologiaDocumento)))
                          .bodyToMono(DocumentTypeResponse.class)
                          .doFinally(signalType -> MDC.setContextMap(mdcContextMap));
    }

    @Override
    public ResponseEntity<DocumentType> postdocTypes(DocumentType docTypes) throws IdClientNotFoundException {
        return null;
    }

    @Override
    public ResponseEntity<DocumentType> updatedocTypes(DocumentType docTypes) throws IdClientNotFoundException {
        return null;
    }

    @Override
    public ResponseEntity<DocumentType> deletedocTypes(String tipoDocumento) throws IdClientNotFoundException {
        return null;
    }
}

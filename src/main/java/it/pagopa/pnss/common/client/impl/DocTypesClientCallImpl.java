package it.pagopa.pnss.common.client.impl;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentTypeResponse;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class DocTypesClientCallImpl implements DocTypesClientCall {

    private final WebClient ssWebClient;

    @Value("${gestore.repository.anagrafica.internal.docTypes}")
    private String anagraficaDocTypesInternalClientEndpoint;

    public DocTypesClientCallImpl(WebClient ssWebClient) {
        this.ssWebClient = ssWebClient;
    }

    @Override
    public Mono<DocumentTypeResponse> getdocTypes(String tipologiaDocumento) throws IdClientNotFoundException {
        return ssWebClient.get()
                          .uri(String.format(anagraficaDocTypesInternalClientEndpoint, tipologiaDocumento))
                          .retrieve()
                          .bodyToMono(DocumentTypeResponse.class);
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

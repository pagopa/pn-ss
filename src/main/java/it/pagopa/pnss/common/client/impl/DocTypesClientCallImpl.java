package it.pagopa.pnss.common.client.impl;

import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.dto.DocTypesInput;
import it.pagopa.pnss.repositoryManager.dto.DocTypesOutput;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

//@Service
public class DocTypesClientCallImpl implements DocTypesClientCall {
    private final WebClient ecInternalWebClient= WebClient.builder().build();

    @Value("${gestore.repository.anagrafica.docTypes}")
    String anagraficaDocTypesClientEndpoint;



    @Override
    public ResponseEntity<DocTypesOutput> getdocTypes(String tipologiaDocumento) throws IdClientNotFoundException {
        return ecInternalWebClient.get()
                .uri(String.format(anagraficaDocTypesClientEndpoint, tipologiaDocumento))
                .retrieve()
                .bodyToMono(ResponseEntity.class).block();
    }

    @Override
    public Mono<ResponseEntity<DocTypesOutput>> postdocTypes(DocTypesInput docTypes) throws IdClientNotFoundException {
        return null;
    }

    @Override
    public Mono<ResponseEntity<DocTypesOutput>> updatedocTypes(DocTypesInput docTypes) throws IdClientNotFoundException {
        return null;
    }

    @Override
    public Mono<ResponseEntity<DocTypesOutput>> deletedocTypes(String checksum) throws IdClientNotFoundException {
        return null;
    }
}

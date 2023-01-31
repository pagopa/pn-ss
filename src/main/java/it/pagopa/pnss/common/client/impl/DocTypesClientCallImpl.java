package it.pagopa.pnss.common.client.impl;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import reactor.core.publisher.Mono;

//@Service
public class DocTypesClientCallImpl extends CommonBaseClient implements DocTypesClientCall {
    private final WebClient.Builder ecInternalWebClient= WebClient.builder();

    @Value("${gestore.repository.anagrafica.docTypes}")
    String anagraficaDocTypesClientEndpoint;



    @Override
    public ResponseEntity<DocumentType>  getdocTypes(String tipologiaDocumento) throws IdClientNotFoundException {
        return getWebClient().get()
                .uri(String.format(anagraficaDocTypesClientEndpoint, tipologiaDocumento))
                .retrieve()
                .bodyToMono(ResponseEntity.class).block();
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
    public ResponseEntity<DocumentType> deletedocTypes(String checksum) throws IdClientNotFoundException {
        return null;
    }

    public WebClient getWebClient(){
        WebClient.Builder builder = enrichBuilder(ecInternalWebClient);
        return builder.build();
    }
}

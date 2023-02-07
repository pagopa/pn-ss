package it.pagopa.pnss.common.client.impl;

import com.amazonaws.services.dynamodbv2.xspec.S;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pnss.common.client.DocumentClientCall;

import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class DocumentClientCallImpl extends CommonBaseClient implements DocumentClientCall {
    private final WebClient.Builder ecInternalWebClient= WebClient.builder();

    @Value("${gestore.repository.anagrafica.internal.docClient}")
    String anagraficaDocumentiClientEndpoint;



    @Override
    public ResponseEntity<Document> getdocument(String keyFile) throws IdClientNotFoundException {
        return getWebClient().get()
                .uri(String.format(anagraficaDocumentiClientEndpoint, keyFile))
                .retrieve()
                .bodyToMono(ResponseEntity.class).block();
    }

    @Override
    public ResponseEntity<Document> postdocument(Document Document) throws IdClientNotFoundException {
        return getWebClient().post()
                .uri(String.format(anagraficaDocumentiClientEndpoint))
                .bodyValue(Document)
                .retrieve()
                .bodyToMono(ResponseEntity.class).block();
    }

    @Override
    public ResponseEntity<Document> updatedocument(Document Document) throws IdClientNotFoundException {
        return getWebClient().put()
                .uri(String.format(anagraficaDocumentiClientEndpoint))
                .bodyValue(Document)
                .retrieve()
                .bodyToMono(ResponseEntity.class).block();
    }

    @Override
    public ResponseEntity<Document> patchdocument(String keyFile, Document document) throws IdClientNotFoundException {
        return getWebClient().patch()
                .uri(String.format(anagraficaDocumentiClientEndpoint, document.getDocumentKey()))
                .bodyValue(document)
                .retrieve()
                .bodyToMono(ResponseEntity.class).block();
    }

    @Override
    public ResponseEntity<Document> deletedocument(String keyFile) throws IdClientNotFoundException {
        return null;
    }
    public WebClient getWebClient(){
        WebClient.Builder builder = enrichBuilder(ecInternalWebClient);
        return builder.build();
    }

}

package it.pagopa.pnss.common.client.impl;

import static org.springframework.http.HttpStatus.FORBIDDEN;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentChanges;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentInput;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentResponse;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.DocumentkeyPresentException;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import reactor.core.publisher.Mono;

@Service
public class DocumentClientCallImpl extends CommonBaseClient implements DocumentClientCall {
    private final WebClient.Builder ecInternalWebClient= WebClient.builder();

    @Value("${gestore.repository.anagrafica.internal.docClient}")
    String anagraficaDocumentiClientEndpoint;
    @Value("${gestore.repository.anagrafica.internal.docClient.post}")
    String anagraficaDocumentiClientEndpointpost;

    @Value("${header.x-api-key}")
    private String xApiKey;
    @Value("${header.x-pagopa-safestorage-cx-id}")
    private String xPagopaSafestorageCxId;
    
    public WebClient getWebClient(){
        WebClient.Builder builder = enrichBuilder(ecInternalWebClient);
        return builder.baseUrl("http://localhost:8080").build();
    }


    @Override
    public Mono<DocumentResponse> getdocument(String keyFile) throws IdClientNotFoundException {
        return getWebClient().get()
                .uri(String.format(anagraficaDocumentiClientEndpoint, keyFile))
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals, clientResponse -> Mono.error(new DocumentKeyNotPresentException(keyFile)))
                .bodyToMono(DocumentResponse.class);
    }

    @Override
    public Mono<DocumentResponse> postDocument(DocumentInput document) throws IdClientNotFoundException {
        return getWebClient().post()
                .uri(anagraficaDocumentiClientEndpointpost)
                .bodyValue(document)
                .retrieve()
                .onStatus(FORBIDDEN::equals, clientResponse -> Mono.error(new DocumentkeyPresentException(document.getDocumentKey())))
                .bodyToMono(DocumentResponse.class);
    }

    @Override
    public ResponseEntity<Document> updatedocument(Document document) throws IdClientNotFoundException {
        return getWebClient().put()
                .uri(String.format(anagraficaDocumentiClientEndpoint))
                .bodyValue(document)
                .retrieve()
                .bodyToMono(ResponseEntity.class).block();
    }

    @Override
    public Mono<DocumentResponse> patchdocument(
    		String authPagopaSafestorageCxId, String authApiKey, 
    		String keyFile, DocumentChanges document) throws IdClientNotFoundException {
        return getWebClient().patch()
                .uri(String.format(anagraficaDocumentiClientEndpoint, keyFile))
        		.header(xPagopaSafestorageCxId, authPagopaSafestorageCxId)
        		.header(xApiKey, authApiKey)
                .bodyValue(document)
                .retrieve()
                .bodyToMono(DocumentResponse.class);
    }

    @Override
    public ResponseEntity<Document> deletedocument(String keyFile) throws IdClientNotFoundException {
        return null;
    }

}

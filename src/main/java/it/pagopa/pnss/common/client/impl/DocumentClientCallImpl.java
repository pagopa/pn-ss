package it.pagopa.pnss.common.client.impl;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentChanges;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentInput;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentResponse;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.DocumentkeyPresentException;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.common.exception.InvalidNextStatusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.springframework.http.HttpStatus.*;

@Service
@Slf4j
public class DocumentClientCallImpl implements DocumentClientCall {

    @Value("${gestore.repository.anagrafica.internal.docClient}")
    private String anagraficaDocumentiClientEndpoint;

    @Value("${gestore.repository.anagrafica.internal.docClient.post}")
    private String anagraficaDocumentiClientEndpointPost;

    @Value("${header.x-api-key}")
    private String xApiKey;

    @Value("${header.x-pagopa-safestorage-cx-id}")
    private String xPagopaSafestorageCxId;

    private final WebClient ssWebClient;

    public DocumentClientCallImpl(WebClient ssWebClient) {
        this.ssWebClient = ssWebClient;
    }

    @Override
    public Mono<DocumentResponse> getDocument(String keyFile) throws DocumentKeyNotPresentException {
        return ssWebClient.get()
                          .uri(String.format(anagraficaDocumentiClientEndpoint, URLDecoder.decode(keyFile, StandardCharsets.UTF_8)))
                          .retrieve()
                          .onStatus(HttpStatus.NOT_FOUND::equals, clientResponse -> Mono.error(new DocumentKeyNotPresentException(keyFile)))
                          .bodyToMono(DocumentResponse.class);
    }

    @Override
    public Mono<DocumentResponse> postDocument(DocumentInput document) throws DocumentkeyPresentException {
        return ssWebClient.post()
                          .uri(anagraficaDocumentiClientEndpointPost)
                          .bodyValue(document)
                          .retrieve()
                          .onStatus(FORBIDDEN::equals,
                                    clientResponse -> Mono.error(new DocumentkeyPresentException(document.getDocumentKey())))
                          .bodyToMono(DocumentResponse.class);
    }

    @Override
    public Mono<DocumentResponse> patchDocument(String authPagopaSafestorageCxId, String authApiKey, String keyFile,
                                                DocumentChanges document)
            throws DocumentKeyNotPresentException {

        String decodedKeyFile = URLDecoder.decode(keyFile, StandardCharsets.UTF_8);

        return ssWebClient.patch()
                          .uri(String.format(anagraficaDocumentiClientEndpoint, decodedKeyFile))
                          .header(xPagopaSafestorageCxId, authPagopaSafestorageCxId)
                          .header(xApiKey, authApiKey)
                          .bodyValue(document)
                          .retrieve()
                          .onStatus(BAD_REQUEST::equals,
                                    clientResponse -> Mono.error(new InvalidNextStatusException(document.getDocumentState(), decodedKeyFile)))
                          .onStatus(NOT_FOUND::equals,
                                    clientResponse -> Mono.error(new DocumentKeyNotPresentException(decodedKeyFile)))
                          .bodyToMono(DocumentResponse.class);
    }

    @Override
    public ResponseEntity<Document> deleteDocument(String keyFile) throws IdClientNotFoundException {
        return null;
    }
}

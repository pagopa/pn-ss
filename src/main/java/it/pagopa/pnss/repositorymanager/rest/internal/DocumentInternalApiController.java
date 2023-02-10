package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pn.template.internal.rest.v1.api.DocumentInternalApi;
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentResponse;
import it.pagopa.pn.template.internal.rest.v1.dto.Error;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class DocumentInternalApiController implements DocumentInternalApi {

    private final DocumentService documentService;

    public DocumentInternalApiController(DocumentService documentService) {
        this.documentService = documentService;
    }

    private DocumentResponse getResponse(Document document) {
        DocumentResponse response = new DocumentResponse();
        response.setDocument(document);
        return response;
    }

    private Mono<ResponseEntity<DocumentResponse>> getResponse(String documentKey, Throwable throwable) {
        DocumentResponse response = new DocumentResponse();
        response.setError(new Error());

        response.getError().setDescription(throwable.getMessage());

        if (throwable instanceof ItemAlreadyPresent) {
            response.getError()
                    .setDescription(documentKey == null ? "Document already present" : String.format("Doc with id %s already present",
                                                                                                     documentKey));
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(response));
        } else if (throwable instanceof DocumentKeyNotPresentException) {
            response.getError()
                    .setDescription(documentKey == null ? "Document not found" : String.format("Doc with id %s not found", documentKey));
            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(response));
        } else if (throwable instanceof RepositoryManagerException) {
            response.getError().setDescription("Document has incorrect attribute");
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
        } else {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
        }
    }

    @Override
    public Mono<ResponseEntity<DocumentResponse>> getDocument(String documentKey, final ServerWebExchange exchange) {

        return documentService.getDocument(documentKey)
                              .map(documentOutput -> ResponseEntity.ok(getResponse(documentOutput)))
                              .onErrorResume(throwable -> getResponse(documentKey, throwable));

    }

    @Override
    public Mono<ResponseEntity<DocumentResponse>> insertDocument(Mono<Document> document, final ServerWebExchange exchange) {

        return document.flatMap(documentService::insertDocument)
                       .map(documentOutput -> ResponseEntity.ok(getResponse(documentOutput)))
                       .onErrorResume(throwable -> getResponse(null, throwable));

    }

    @Override
    public Mono<ResponseEntity<DocumentResponse>> patchDoc(String documentKey, Mono<Document> document, final ServerWebExchange exchange) {

        return document.flatMap(request -> documentService.patchDocument(documentKey, request))
                       .map(documentOutput -> ResponseEntity.ok(getResponse(documentOutput)))
                       .onErrorResume(throwable -> getResponse(documentKey, throwable));

    }

    @Override
    public Mono<ResponseEntity<DocumentResponse>> deleteDocument(String documentKey, final ServerWebExchange exchange) {

        return documentService.deleteDocument(documentKey)
                              .map(documentOutput -> ResponseEntity.ok(getResponse(documentOutput)))
                              .onErrorResume(throwable -> getResponse(documentKey, throwable));

    }
}

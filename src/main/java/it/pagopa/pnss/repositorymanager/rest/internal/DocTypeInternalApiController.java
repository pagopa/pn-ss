package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pn.template.internal.rest.v1.api.DocTypeInternalApi;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentTypeResponse;
import it.pagopa.pn.template.internal.rest.v1.dto.Error;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class DocTypeInternalApiController implements DocTypeInternalApi {

    private final DocTypesService docTypesService;

    public DocTypeInternalApiController(DocTypesService docTypesService) {
        this.docTypesService = docTypesService;
    }

    private DocumentTypeResponse getResponse(DocumentType docType) {
        DocumentTypeResponse response = new DocumentTypeResponse();
        response.setDocType(docType);
        return response;
    }

    private Mono<ResponseEntity<DocumentTypeResponse>> getResponse(String typeId, Throwable throwable) {
        DocumentTypeResponse response = new DocumentTypeResponse();
        response.setError(new Error());

        response.getError().setDescription(throwable.getMessage());

        if (throwable instanceof ItemAlreadyPresent) {
            response.getError()
                    .setDescription(
                            typeId == null ? "DocType already present" : String.format("DocType with id %s already present", typeId));
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(response));
        } else if (throwable instanceof DocumentTypeNotPresentException) {
            response.getError()
                    .setDescription(typeId == null ? "Document type not found" : String.format("Doc with id %s not found", typeId));
            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(response));
        } else if (throwable instanceof RepositoryManagerException) {
            response.getError().setDescription("DocType has incorrect attribute");
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
        } else {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
        }
    }

    @Override
    public Mono<ResponseEntity<DocumentTypeResponse>> getDocType(String typeId, final ServerWebExchange exchange) {

        return docTypesService.getDocType(typeId)
                              .map(docType -> ResponseEntity.ok(getResponse(docType)))
                              .onErrorResume(throwable -> getResponse(typeId, throwable));
    }

    @Override
    public Mono<ResponseEntity<DocumentTypeResponse>> insertDocType(Mono<DocumentType> documentType, final ServerWebExchange exchange) {

        return documentType.flatMap(docTypesService::insertDocType)
                           .map(docType -> ResponseEntity.ok(getResponse(docType)))
                           .onErrorResume(throwable -> getResponse(null, throwable));

    }

    @Override
    public Mono<ResponseEntity<DocumentTypeResponse>> updateDocType(String typeId, Mono<DocumentType> documentType,
                                                                    final ServerWebExchange exchange) {

        return documentType.flatMap(request -> docTypesService.updateDocType(typeId, request))
                           .map(docType -> ResponseEntity.ok(getResponse(docType)))
                           .onErrorResume(throwable -> getResponse(typeId, throwable));

    }

    @Override
    public Mono<ResponseEntity<DocumentTypeResponse>> deleteDocType(String typeId, final ServerWebExchange exchange) {

        return docTypesService.deleteDocType(typeId)
                              .map(docType -> ResponseEntity.ok(getResponse(docType)))
                              .onErrorResume(throwable -> getResponse(typeId, throwable));

    }
}

package it.pagopa.pnss.common.client;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentTypeResponse;
import org.springframework.http.ResponseEntity;


import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import reactor.core.publisher.Mono;

public interface DocTypesClientCall {

    Mono<DocumentTypeResponse>  getdocTypes(String tipologiaDocumento)throws IdClientNotFoundException;

    ResponseEntity<DocumentType> postdocTypes(DocumentType docTypes) throws IdClientNotFoundException;

    ResponseEntity<DocumentType> updatedocTypes(DocumentType docTypes) throws IdClientNotFoundException;

    ResponseEntity<DocumentType> deletedocTypes(String tipoDocumento) throws IdClientNotFoundException;

}

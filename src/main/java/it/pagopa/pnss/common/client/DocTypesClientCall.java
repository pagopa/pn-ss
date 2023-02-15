package it.pagopa.pnss.common.client;

import org.springframework.http.ResponseEntity;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;

public interface DocTypesClientCall {

    ResponseEntity<DocumentType>  getdocTypes(String tipologiaDocumento)throws IdClientNotFoundException;

    ResponseEntity<DocumentType> postdocTypes(DocumentType docTypes) throws IdClientNotFoundException;

    ResponseEntity<DocumentType> updatedocTypes(DocumentType docTypes) throws IdClientNotFoundException;

    ResponseEntity<DocumentType> deletedocTypes( String checksum) throws IdClientNotFoundException;

}

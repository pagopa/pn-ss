package it.pagopa.pnss.common.client;

import it.pagopa.pnss.common.client.dto.DocumentDTO;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface DocumentClientCall {

    ResponseEntity<DocumentDTO> getdocument(String keyFile) throws IdClientNotFoundException;
    ResponseEntity<DocumentDTO> postdocument(DocumentDTO DocumentDTO) throws IdClientNotFoundException;

    ResponseEntity<DocumentDTO> updatedocument( DocumentDTO document) throws IdClientNotFoundException;

    ResponseEntity<DocumentDTO> deletedocument(String keyFile) throws IdClientNotFoundException;
}

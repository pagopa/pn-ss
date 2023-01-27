package it.pagopa.pnss.common.client;

import org.springframework.http.ResponseEntity;

import it.pagopa.pnss.common.client.dto.DocumentInput;
import it.pagopa.pnss.common.client.dto.DocumentOutput;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;

public interface DocumentClientCall {

    ResponseEntity<DocumentOutput> getdocument(String keyFile) throws IdClientNotFoundException;
    ResponseEntity<DocumentOutput> postdocument(DocumentInput documentInput) throws IdClientNotFoundException;

    ResponseEntity<DocumentOutput> updatedocument( DocumentInput document) throws IdClientNotFoundException;

    ResponseEntity<DocumentOutput> deletedocument(String keyFile) throws IdClientNotFoundException;
}

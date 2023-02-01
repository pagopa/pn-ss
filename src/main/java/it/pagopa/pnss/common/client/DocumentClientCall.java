package it.pagopa.pnss.common.client;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface DocumentClientCall {

    ResponseEntity<Document> getdocument(String keyFile) throws IdClientNotFoundException;
    ResponseEntity<Document> postdocument(Document Document) throws IdClientNotFoundException;

    ResponseEntity<Document> updatedocument( Document document) throws IdClientNotFoundException;

    ResponseEntity<Document> deletedocument(String keyFile) throws IdClientNotFoundException;
}

package it.pagopa.pnss.common.client;

import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.dto.DocumentInput;
import it.pagopa.pnss.repositoryManager.dto.DocumentOutput;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface DocumentClientCall {

    ResponseEntity<DocumentOutput> getdocument(String keyFile) throws IdClientNotFoundException;
    ResponseEntity<DocumentOutput> postdocument(DocumentInput documentInput) throws IdClientNotFoundException;

    ResponseEntity<DocumentOutput> updatedocument( DocumentInput document) throws IdClientNotFoundException;

    ResponseEntity<DocumentOutput> deletedocument(String keyFile) throws IdClientNotFoundException;
}

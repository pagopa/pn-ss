package it.pagopa.pnss.common.client;

import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.dto.DocTypesInput;
import it.pagopa.pnss.repositoryManager.dto.DocTypesOutput;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

public interface DocTypesClientCall {

    ResponseEntity<DocTypesOutput> getdocTypes( String tipologiaDocumento)throws IdClientNotFoundException;

    Mono<ResponseEntity<DocTypesOutput>> postdocTypes(DocTypesInput docTypes) throws IdClientNotFoundException;

    Mono<ResponseEntity<DocTypesOutput>> updatedocTypes(DocTypesInput docTypes) throws IdClientNotFoundException;

    Mono<ResponseEntity<DocTypesOutput>> deletedocTypes( String checksum) throws IdClientNotFoundException;

}

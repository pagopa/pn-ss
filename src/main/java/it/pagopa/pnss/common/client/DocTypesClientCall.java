package it.pagopa.pnss.common.client;

import org.springframework.http.ResponseEntity;

import it.pagopa.pnss.common.client.dto.DocTypesInput;
import it.pagopa.pnss.common.client.dto.DocTypesOutput;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import reactor.core.publisher.Mono;

public interface DocTypesClientCall {

    ResponseEntity<DocTypesOutput> getdocTypes( String tipologiaDocumento)throws IdClientNotFoundException;

    Mono<ResponseEntity<DocTypesOutput>> postdocTypes(DocTypesInput docTypes) throws IdClientNotFoundException;

    Mono<ResponseEntity<DocTypesOutput>> updatedocTypes(DocTypesInput docTypes) throws IdClientNotFoundException;

    Mono<ResponseEntity<DocTypesOutput>> deletedocTypes( String checksum) throws IdClientNotFoundException;

}

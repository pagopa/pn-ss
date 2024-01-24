package it.pagopa.pnss.common.client;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentTTLInput;
import reactor.core.publisher.Mono;

public interface DocumentTTLClientCall {

    Mono<Void> insertDocumentTTL(DocumentTTLInput documentTTLInput);

}

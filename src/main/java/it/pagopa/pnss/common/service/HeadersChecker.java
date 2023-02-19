package it.pagopa.pnss.common.service;

import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pnss.common.client.dto.IdentityCheckResponse;
import reactor.core.publisher.Mono;

public interface HeadersChecker {

	Mono<IdentityCheckResponse> checkIdentity(final ServerWebExchange exchange);
	
}

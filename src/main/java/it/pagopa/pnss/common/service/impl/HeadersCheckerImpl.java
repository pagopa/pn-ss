package it.pagopa.pnss.common.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.internal.rest.v1.dto.UserConfigurationResponse;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.dto.IdentityCheckResponse;
import it.pagopa.pnss.common.client.exception.HeaderCheckException;
import it.pagopa.pnss.common.client.exception.HeaderNotFoundException;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.common.client.exception.IdentityCheckFailException;
import it.pagopa.pnss.common.service.HeadersChecker;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class HeadersCheckerImpl implements HeadersChecker {
	
	@Value("${header.x-api-key:#{null}}")
	private String xApiKey;
	
	@Value("${header.x-pagopa-safestorage-cx-id:#{null}}")
	private String xPagopaSafestorageCxId;
	
	private final UserConfigurationClientCall userConfigurationClientCall;

	public HeadersCheckerImpl(UserConfigurationClientCall userConfigurationClientCall) {
		this.userConfigurationClientCall = userConfigurationClientCall;
	}
	
	private Mono<IdentityCheckResponse> checkIdentity(String valueForXApiKey, String valueForxPagopaSafestorageCxId, UserConfigurationResponse ucResponse) {
		if (ucResponse.getError() != null 
				&& ( (ucResponse.getError().getCode() != null && !ucResponse.getError().getCode().isBlank()) 
						|| (ucResponse.getError().getDescription() != null && !ucResponse.getError().getDescription().isBlank()))) {
			return Mono.error(new IdClientNotFoundException(valueForxPagopaSafestorageCxId));
		}
		if (ucResponse.getUserConfiguration() == null || (ucResponse.getUserConfiguration() != null && 
				(ucResponse.getUserConfiguration().getApiKey() == null || ucResponse.getUserConfiguration().getApiKey().isEmpty()))) {
			return Mono.error(new IdClientNotFoundException(valueForxPagopaSafestorageCxId));
		}
		String clientApiKey = ucResponse.getUserConfiguration().getApiKey();
		log.info("checkIdentity() : clientApiKey {}", clientApiKey);
		if (!clientApiKey.equals(valueForXApiKey)) {
			return Mono.error(new IdentityCheckFailException("Access denied"));
		}
		return Mono.just(new IdentityCheckResponse(clientApiKey));
	}
	
	@Override
	public Mono<IdentityCheckResponse> checkIdentity(final ServerWebExchange exchange) {
		log.info("checkIdentity() : START");
		
		if (exchange == null || xApiKey == null || xPagopaSafestorageCxId == null) {
			log.error("checkIdentity(): Header not found");
			throw new HeaderCheckException("Header not found");
		}
		List<String> xApiKeyList = exchange.getRequest().getHeaders().get(xApiKey);
		List<String> xPagopaSafestorageCxIdList = exchange.getRequest().getHeaders().get(xPagopaSafestorageCxId);
		log.info("checkIdentity() : xApiKeyList {} : xPagopaSafestorageCxIdList {}", xApiKeyList, xPagopaSafestorageCxIdList);
		if (xApiKeyList == null || xApiKeyList.isEmpty() || xApiKeyList.size() > 1) {
			return Mono.error(new HeaderNotFoundException(xApiKey));
		}
		if (xPagopaSafestorageCxIdList == null || xPagopaSafestorageCxIdList.isEmpty() || xPagopaSafestorageCxIdList.size() > 1) {
			return Mono.error(new HeaderNotFoundException(xPagopaSafestorageCxId));
		}
		
		String valueForXApiKey = xApiKeyList.get(0);
		String valueForxPagopaSafestorageCxId = xPagopaSafestorageCxIdList.get(0);
		log.info("checkIdentity() : valueForXApiKey {} : valueForxPagopaSafestorageCxId {}", valueForXApiKey, valueForxPagopaSafestorageCxId);
		
		return userConfigurationClientCall.getUser(valueForxPagopaSafestorageCxId)
				.flatMap(client -> checkIdentity(valueForXApiKey, valueForxPagopaSafestorageCxId, client))
				.onErrorResume(WebClientResponseException.class, error -> Mono.error(new IdClientNotFoundException(valueForxPagopaSafestorageCxId)));
	}

}

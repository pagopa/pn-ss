package it.pagopa.pnss.common.client.impl;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;

import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pn.template.rest.v1.dto.UserConfiguration;
import it.pagopa.pnss.common.client.ConfigurationApiCall;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import reactor.core.publisher.Mono;

public class ConfigurationApiCallImpl implements ConfigurationApiCall {
	
	private final WebClient webClient;
	
	public ConfigurationApiCallImpl(WebClient webClient) {
		super();
		this.webClient = webClient;
	}

	@Value("${gestore.repository.configuration.api.documents.config}")
	private String configurationApiDocumentsConfigClientEndpoint;

	@Override
	public Mono<DocumentTypesConfigurations> getDocumentsConfigs() {
		return webClient.get()
                		.uri(configurationApiDocumentsConfigClientEndpoint)
                        .retrieve()
                        .onStatus(NOT_FOUND::equals,
                                  clientResponse -> Mono.error(new DocumentTypeNotPresentException("Document Types not found")))
                        .onStatus(BAD_REQUEST::equals,
                                  clientResponse -> Mono.error(new DocumentTypeNotPresentException("Storage Configurations not found")))
                        .bodyToMono(DocumentTypesConfigurations.class);
	}

	@Override
	public Mono<UserConfiguration> getCurrentClientConfig(String clientId) throws IdClientNotFoundException {
		return null;
	}

}

package it.pagopa.pnss.common.client.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pn.template.rest.v1.dto.UserConfiguration;
import it.pagopa.pnss.common.client.ConfigurationApiCall;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import reactor.core.publisher.Mono;

@Service
public class ConfigurationApiCallImpl extends CommonBaseClient implements ConfigurationApiCall {
	
	private final WebClient.Builder ecInternalWebClient= WebClient.builder();

	@Value("${gestore.repository.configuration.api.documents.config}")
	private String configurationApiDocumentsConfigClientEndpoint;
	
    public WebClient getWebClient(){
        WebClient.Builder builder = enrichBuilder(ecInternalWebClient);
        return builder.build();
    }

	@Override
	public Mono<DocumentTypesConfigurations> getDocumentsConfigs() {
		return getWebClient().get()
                		.uri(configurationApiDocumentsConfigClientEndpoint)
                        .retrieve()
                        .bodyToMono(DocumentTypesConfigurations.class);
	}

	@Override
	public Mono<UserConfiguration> getCurrentClientConfig(String clientId) throws IdClientNotFoundException {
		return null;
	}

}

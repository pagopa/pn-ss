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
	
    @Value("${header.x-api-key}")
    private String xApiKey;

    @Value("${header.x-pagopa-safestorage-cx-id}")
    private String xPagopaSafestorageCxId;
	
	private final WebClient.Builder ecInternalWebClient= WebClient.builder();

	@Value("${gestore.repository.configuration.api.documents.config}")
	private String configurationApiDocumentsConfigClientEndpoint;
	
    public WebClient getWebClient(){
        WebClient.Builder builder = enrichBuilder(ecInternalWebClient);
        return builder.baseUrl("http://localhost:8080").build();
    }

	@Override
	public Mono<DocumentTypesConfigurations> getDocumentsConfigs(String authPagopaSafestorageCxId, String authApiKey) {
		return getWebClient().get()
                		.uri(configurationApiDocumentsConfigClientEndpoint)
                		.header(xPagopaSafestorageCxId, authPagopaSafestorageCxId)
                		.header(xApiKey, authApiKey)
                        .retrieve()
                        .bodyToMono(DocumentTypesConfigurations.class);
	}

	@Override
	public Mono<UserConfiguration> getCurrentClientConfig(String clientId) throws IdClientNotFoundException {
		return null;
	}

}

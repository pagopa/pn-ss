package it.pagopa.pnss.common.client.impl;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfiguration;
import it.pagopa.pnss.common.client.ConfigurationApiCall;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Service
public class ConfigurationApiCallImpl implements ConfigurationApiCall {

    private final WebClient ssWebClient;

    @Value("${header.x-api-key}")
    private String xApiKey;

    @Value("${header.x-pagopa-safestorage-cx-id}")
    private String xPagopaSafestorageCxId;

    @Value("${gestore.repository.configuration.api.documents.config}")
    private String configurationApiDocumentsConfigClientEndpoint;

    public ConfigurationApiCallImpl(WebClient ssWebClient) {
        this.ssWebClient = ssWebClient;
    }

    @Override
    public Mono<DocumentTypesConfigurations> getDocumentsConfigs(String authPagopaSafestorageCxId, String authApiKey) {
        return ssWebClient.get()
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

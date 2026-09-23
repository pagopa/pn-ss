package it.pagopa.pnss.common.client.impl;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfiguration;
import it.pagopa.pnss.common.client.ConfigurationApiCall;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.configurationproperties.PnSsConfig;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static it.pagopa.pnss.common.utils.LogUtils.*;


@Service
@CustomLog
public class ConfigurationApiCallImpl implements ConfigurationApiCall {

    private final WebClient ssWebClient;

    private final String xApiKey;
    private final String xPagopaSafestorageCxId;
    private final String configurationApiDocumentsConfigClientEndpoint;

    public ConfigurationApiCallImpl(@Qualifier("ssWebClient") WebClient ssWebClient, PnSsConfig pnSsConfig) {
        this.ssWebClient = ssWebClient;
        this.xApiKey = pnSsConfig.getClientInterni().getHeader().getApiKey();
        this.xPagopaSafestorageCxId = pnSsConfig.getClientInterni().getHeader().getPagopaSafestorageCxId();
        this.configurationApiDocumentsConfigClientEndpoint = pnSsConfig.getClientInterni().getEndpoint().getConfigurationApiDocumentsConfig();
    }

    @Override
    public Mono<DocumentTypesConfigurations> getDocumentsConfigs(String authPagopaSafestorageCxId, String authApiKey) {
        log.debug(INVOKING_INTERNAL_SERVICE, REPOSITORY_MANAGER, GET_DOCUMENTS_CONFIGS);
        return ssWebClient.get()
                          .uri(configurationApiDocumentsConfigClientEndpoint)
                          .headers(headers -> {
                              headers.add(xPagopaSafestorageCxId, authPagopaSafestorageCxId);
                              if (StringUtils.isNotBlank(authApiKey)) {
                                  headers.add(xApiKey, authApiKey);
                              }
                          })
                          .retrieve()
                          .bodyToMono(DocumentTypesConfigurations.class);
    }
    @Override
    public Mono<UserConfiguration> getCurrentClientConfig(String clientId) throws IdClientNotFoundException {
        return null;
    }
}

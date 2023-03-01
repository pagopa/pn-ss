package it.pagopa.pnss.common.client;

import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pn.template.rest.v1.dto.UserConfiguration;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import reactor.core.publisher.Mono;

public interface ConfigurationApiCall {
	
	Mono<DocumentTypesConfigurations> getDocumentsConfigs(String authPagopaSafestorageCxId, String authApiKey);
	Mono<UserConfiguration> getCurrentClientConfig(String clientId) throws IdClientNotFoundException;

}

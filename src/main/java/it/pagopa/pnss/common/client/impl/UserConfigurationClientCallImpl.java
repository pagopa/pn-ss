package it.pagopa.pnss.common.client.impl;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfiguration;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfigurationResponse;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.configurationproperties.PnSsConfig;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static it.pagopa.pnss.common.utils.LogUtils.*;


@Service
@CustomLog
public class UserConfigurationClientCallImpl implements UserConfigurationClientCall {

    private final String anagraficaUserConfigurationInternalClientEndpoint;

    private final WebClient ssWebClient;

    public UserConfigurationClientCallImpl(@Qualifier("ssWebClient") WebClient ssWebClient, PnSsConfig pnSsConfig) {
        this.ssWebClient = ssWebClient;
        this.anagraficaUserConfigurationInternalClientEndpoint = pnSsConfig.getClientInterni().getEndpoint().getUserConfiguration();
    }

    @Override
    public Mono<UserConfigurationResponse> getUser(String xPagopaSafestorageCxId) {
        log.debug(INVOKING_INTERNAL_SERVICE, REPOSITORY_MANAGER, GET_USER);
        return ssWebClient.get()
                          .uri(String.format(anagraficaUserConfigurationInternalClientEndpoint, xPagopaSafestorageCxId))
                          .retrieve()
                          .onStatus(HttpStatusCode::is4xxClientError,
                                    clientResponse -> Mono.error(new IdClientNotFoundException(xPagopaSafestorageCxId)))
                          .bodyToMono(UserConfigurationResponse.class);
    }

    @Override
    public ResponseEntity<UserConfiguration> postUser(UserConfiguration user) throws IdClientNotFoundException {
        return null;
    }

    @Override
    public ResponseEntity<UserConfiguration> updateUser(UserConfiguration user) throws IdClientNotFoundException {
        return null;
    }

    @Override
    public ResponseEntity<UserConfiguration> deleteUser(String name) throws IdClientNotFoundException {
        return null;
    }
}

package it.pagopa.pnss.common.client.impl;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfiguration;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfigurationResponse;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import lombok.CustomLog;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static it.pagopa.pnss.common.utils.LogUtils.*;


@Service
@CustomLog
public class UserConfigurationClientCallImpl implements UserConfigurationClientCall {

    @Value("${gestore.repository.anagrafica.internal.userConfiguration}")
    private String anagraficaUserConfigurationInternalClientEndpoint;

    private final WebClient ssWebClient;

    public UserConfigurationClientCallImpl(WebClient ssWebClient) {
        this.ssWebClient = ssWebClient;
    }

    @Override
    public Mono<UserConfigurationResponse> getUser(String xPagopaSafestorageCxId) {
        var mdcContextMap = MDCUtils.retrieveMDCContextMap();
        log.debug(INVOKING_INTERNAL_SERVICE, REPOSITORY_MANAGER, GET_USER);
        return ssWebClient.get()
                          .uri(String.format(anagraficaUserConfigurationInternalClientEndpoint, xPagopaSafestorageCxId))
                          .retrieve()
                          .onStatus(HttpStatus::is4xxClientError,
                                    clientResponse -> Mono.error(new IdClientNotFoundException(xPagopaSafestorageCxId)))
                          .bodyToMono(UserConfigurationResponse.class)
                          .doFinally(signalType -> MDC.setContextMap(mdcContextMap));
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

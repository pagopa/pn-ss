package it.pagopa.pnss.common.client.impl;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.template.rest.v1.dto.UserConfiguration;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class UserConfigurationClientCallImpl extends CommonBaseClient implements UserConfigurationClientCall {

    private final WebClient.Builder ecInternalWebClient= WebClient.builder();

    @Value("${gestore.repository.anagrafica.userConfiguration}")
    String anagraficaUserConfigurationClientEndpoint;

    @Value("${gestore.repository.anagrafica.internal.userConfiguration}")
    String anagraficaUserConfigurationInternalClientEndpoint;

    @Override
    public ResponseEntity<UserConfiguration> getUser(String name) throws IdClientNotFoundException {
        return getWebClient().get()
                .uri(String.format(anagraficaUserConfigurationInternalClientEndpoint, name))
                .retrieve()
                .bodyToMono(ResponseEntity.class).block();
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

    public WebClient getWebClient(){
        WebClient.Builder builder = enrichBuilder(ecInternalWebClient);
        return builder.build();
    }

}

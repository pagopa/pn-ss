package it.pagopa.pnss.common.client.impl;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.dto.UserConfigurationDTO;
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



    @Override
    public ResponseEntity<UserConfigurationDTO> getUser(String name) throws IdClientNotFoundException {
        return getWebClient().get()
                .uri(String.format(anagraficaUserConfigurationClientEndpoint, name))
                .retrieve()
                .bodyToMono(ResponseEntity.class).block();
    }

    @Override
    public ResponseEntity<UserConfigurationDTO> postUser(UserConfigurationDTO user) throws IdClientNotFoundException {
        return null;
    }

    @Override
    public ResponseEntity<UserConfigurationDTO> updateUser(UserConfigurationDTO user) throws IdClientNotFoundException {
        return null;
    }

    @Override
    public ResponseEntity<UserConfigurationDTO> deleteUser(String name) throws IdClientNotFoundException {
        return null;
    }

    public WebClient getWebClient(){
        WebClient.Builder builder = enrichBuilder(ecInternalWebClient);
        return builder.build();
    }

}

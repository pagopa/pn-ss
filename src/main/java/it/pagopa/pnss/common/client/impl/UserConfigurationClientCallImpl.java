package it.pagopa.pnss.common.client.impl;

import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.dto.UserConfigurationInput;
import it.pagopa.pnss.repositoryManager.dto.UserConfigurationOutput;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class UserConfigurationClientCallImpl implements UserConfigurationClientCall {

    private final WebClient ecInternalWebClient= WebClient.builder().build();

    @Value("${gestore.repository.anagrafica.userConfiguration}")
    String anagraficaUserConfigurationClientEndpoint;



    @Override
    public ResponseEntity<UserConfigurationOutput> getUser(String name) throws IdClientNotFoundException {
        return ecInternalWebClient.get()
                .uri(String.format(anagraficaUserConfigurationClientEndpoint, name))
                .retrieve()
                .bodyToMono(ResponseEntity.class).block();
    }

    @Override
    public ResponseEntity<UserConfigurationOutput> postUser(UserConfigurationInput user) throws IdClientNotFoundException {
        return null;
    }

    @Override
    public ResponseEntity<UserConfigurationOutput> updateUser(UserConfigurationInput user) throws IdClientNotFoundException {
        return null;
    }

    @Override
    public ResponseEntity<UserConfigurationOutput> deleteUser(String name) throws IdClientNotFoundException {
        return null;
    }
}

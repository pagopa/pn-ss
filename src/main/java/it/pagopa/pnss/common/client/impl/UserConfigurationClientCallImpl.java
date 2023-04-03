package it.pagopa.pnss.common.client.impl;

//import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfigurationResponse;
import it.pagopa.pn.template.rest.v1.dto.UserConfiguration;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Service
@Slf4j
public class UserConfigurationClientCallImpl extends CommonBaseClient implements UserConfigurationClientCall {

    private final WebClient.Builder ecInternalWebClient = WebClient.builder();

    @Value("${gestore.repository.anagrafica.internal.userConfiguration}")
    String anagraficaUserConfigurationInternalClientEndpoint;

    @Autowired
    private final WebClient ssWebClient;

    public UserConfigurationClientCallImpl(WebClient ssWebClient) {
        this.ssWebClient = ssWebClient;
    }

    @Override
    public Mono<UserConfigurationResponse> getUser(String xPagopaSafestorageCxId) {
        return ssWebClient.get()
                .uri(String.format(anagraficaUserConfigurationInternalClientEndpoint, xPagopaSafestorageCxId))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(new IdClientNotFoundException(xPagopaSafestorageCxId)))
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

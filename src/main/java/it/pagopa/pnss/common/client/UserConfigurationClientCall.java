package it.pagopa.pnss.common.client;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfiguration;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfigurationResponse;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface UserConfigurationClientCall {

    Mono<UserConfigurationResponse> getUser(String name) throws IdClientNotFoundException;

    ResponseEntity<UserConfiguration> postUser(UserConfiguration user) throws IdClientNotFoundException;

    ResponseEntity<UserConfiguration> updateUser(UserConfiguration user) throws IdClientNotFoundException;

    ResponseEntity<UserConfiguration> deleteUser(String name) throws IdClientNotFoundException;

}

package it.pagopa.pnss.common.client;

import it.pagopa.pnss.common.client.dto.UserConfigurationDTO;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface UserConfigurationClientCall {

    ResponseEntity<UserConfigurationDTO> getUser(String name) throws IdClientNotFoundException;

    ResponseEntity<UserConfigurationDTO> postUser(UserConfigurationDTO user) throws IdClientNotFoundException;

    ResponseEntity<UserConfigurationDTO> updateUser(UserConfigurationDTO user) throws IdClientNotFoundException;

    ResponseEntity<UserConfigurationDTO> deleteUser(String name) throws IdClientNotFoundException;

}

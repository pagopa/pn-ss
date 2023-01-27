package it.pagopa.pnss.common.client;

import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.dto.UserConfigurationInput;
import it.pagopa.pnss.repositoryManager.dto.UserConfigurationOutput;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface UserConfigurationClientCall {

    ResponseEntity<UserConfigurationOutput> getUser(String name) throws IdClientNotFoundException;

    ResponseEntity<UserConfigurationOutput> postUser(UserConfigurationInput user) throws IdClientNotFoundException;

    ResponseEntity<UserConfigurationOutput> updateUser(UserConfigurationInput user) throws IdClientNotFoundException;

    ResponseEntity<UserConfigurationOutput> deleteUser(String name) throws IdClientNotFoundException;

}

package it.pagopa.pnss.common.client;

import org.springframework.http.ResponseEntity;

import it.pagopa.pnss.common.client.dto.UserConfigurationInput;
import it.pagopa.pnss.common.client.dto.UserConfigurationOutput;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;

public interface UserConfigurationClientCall {

    ResponseEntity<UserConfigurationOutput> getUser(String name) throws IdClientNotFoundException;

    ResponseEntity<UserConfigurationOutput> postUser(UserConfigurationInput user) throws IdClientNotFoundException;

    ResponseEntity<UserConfigurationOutput> updateUser(UserConfigurationInput user) throws IdClientNotFoundException;

    ResponseEntity<UserConfigurationOutput> deleteUser(String name) throws IdClientNotFoundException;

}

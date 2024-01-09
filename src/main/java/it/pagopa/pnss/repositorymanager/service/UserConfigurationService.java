package it.pagopa.pnss.repositorymanager.service;


import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfiguration;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfigurationChanges;
import reactor.core.publisher.Mono;

public interface UserConfigurationService {

	Mono<UserConfiguration> getUserConfiguration(String name);
	Mono<UserConfiguration> insertUserConfiguration(UserConfiguration userConfiguration);
	Mono<UserConfiguration> patchUserConfiguration(String name, UserConfigurationChanges userConfigurationChanges);
	Mono<UserConfiguration> deleteUserConfiguration(String name);

}

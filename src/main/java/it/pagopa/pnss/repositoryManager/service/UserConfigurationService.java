package it.pagopa.pnss.repositoryManager.service;

import it.pagopa.pn.template.internal.rest.v1.dto.UserConfiguration;
import reactor.core.publisher.Mono;

public interface UserConfigurationService {

	Mono<UserConfiguration> getUserConfiguration(String name);
	Mono<UserConfiguration> insertUserConfiguration(UserConfiguration userConfiguration);
	Mono<UserConfiguration> patchUserConfiguration(String name, UserConfiguration userConfiguration);
	Mono<UserConfiguration> deleteUserConfiguration(String name);

}

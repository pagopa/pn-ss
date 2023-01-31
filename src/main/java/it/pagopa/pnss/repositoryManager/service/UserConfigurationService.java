package it.pagopa.pnss.repositoryManager.service;

import it.pagopa.pn.template.internal.rest.v1.dto.UserConfiguration;

public interface UserConfigurationService {

	UserConfiguration getUserConfiguration(String name);
	UserConfiguration insertUserConfiguration(UserConfiguration userConfiguration);
	UserConfiguration patchUserConfiguration(String name, UserConfiguration userConfiguration);
	void deleteUserConfiguration(String name);

}

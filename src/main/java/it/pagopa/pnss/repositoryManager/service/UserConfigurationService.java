package it.pagopa.pnss.repositoryManager.service;

import it.pagopa.pn.template.internal.rest.v1.dto.UserConfiguration;

public interface UserConfigurationService {

	UserConfiguration getUserConfiguration(String username);
	UserConfiguration insertUserConfiguration(UserConfiguration userConfigurationInput);
	UserConfiguration patchUserConfiguration(String username, UserConfiguration userConfigurationInput);
	void deleteUserConfiguration(String username);

}

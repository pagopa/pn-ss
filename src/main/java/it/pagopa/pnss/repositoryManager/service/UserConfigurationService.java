package it.pagopa.pnss.repositoryManager.service;

import it.pagopa.pnss.repositoryManager.dto.UserConfigurationInput;
import it.pagopa.pnss.repositoryManager.dto.UserConfigurationOutput;

public interface UserConfigurationService {
	
	 UserConfigurationOutput getUser(String name);
	 UserConfigurationOutput postUser(UserConfigurationInput userInput);
	 UserConfigurationOutput updateUser(UserConfigurationInput user);
	 UserConfigurationOutput deleteUser(String name);

}

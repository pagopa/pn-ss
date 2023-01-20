package it.pagopa.pnss.repositoryManager.dto;

import it.pagopa.pn.template.rest.v1.dto.UserConfiguration;

public class UserConfigurationOutput {
	
	UserConfiguration userResponse;

	public UserConfiguration getUserResponse() {
		return userResponse;
	}

	public void setUserResponse(UserConfiguration userResponse) {
		this.userResponse = userResponse;
	}
	
	

}

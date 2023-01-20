package it.pagopa.pnss.repositoryManager.service;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.google.gson.Gson;

import it.pagopa.pnss.repositoryManager.dto.UserConfigurationDestinationDTO;

public class UserConfigurationDestinationDTOConverter implements DynamoDBTypeConverter<String, UserConfigurationDestinationDTO> {
    
	@Override
	public String convert(UserConfigurationDestinationDTO destination) {
		
		return new Gson().toJson(destination);
	}

	@Override
	public UserConfigurationDestinationDTO unconvert(String destination) {
		
		return new Gson().fromJson(destination, UserConfigurationDestinationDTO.class);
	}
}

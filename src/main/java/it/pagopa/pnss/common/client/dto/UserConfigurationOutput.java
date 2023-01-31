package it.pagopa.pnss.common.client.dto;

import java.util.List;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class UserConfigurationOutput {
	
	private String username;
	private List<String> canCreate;
	private List<String> canRead;
	private String signatureInfo;
	private UserConfigurationDestinationDTO destination; 
	private String apiKey;

}

package it.pagopa.pnss.common.client.dto;

import java.io.Serializable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class IdentityCheckResponse implements Serializable{
	
	private static final long serialVersionUID = -4402749936090812348L;
	
	String apiKey;

}

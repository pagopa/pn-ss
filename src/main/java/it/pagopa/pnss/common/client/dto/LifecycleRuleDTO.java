package it.pagopa.pnss.common.client.dto;

import java.io.Serializable;

import lombok.AccessLevel;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Data
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LifecycleRuleDTO implements Serializable {

	private static final long serialVersionUID = -7879103125067342862L;
	
	String id;
	Integer expirationDays;
	Integer transitionDays;

}

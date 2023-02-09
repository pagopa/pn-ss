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
	
	String name;
	/** Expiration (retentionPeriod). Example: "aaad bbby" (aaa=number, d=days, bbb=number, y=years) */
	String expirationDays;
	/** Transition, or Espiration  if Transition is empty (hotPeriod). Example: "aaad bbby" (aaa=numeber, d=days, bbb=number, y=years) */
	String transitionDays;

}

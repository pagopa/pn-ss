package it.pagopa.pnss.common.client.dto;

public enum DocumentStateEnumDTO {

	BOOKED("Booked"),
	STAGED("Staged"),
	VALID("Valid"),
	AVAILABLE("Available"),
	FREEZED("Freezed "),
	ATTACHED("Attached "),
	TODELETE("toDelete"),
	DELETED("Deleted ");
	
	private String value;
	
	private DocumentStateEnumDTO(String value) {
		this.value = value;
	}
	
	public String value() {
		return this.value;
	}
}

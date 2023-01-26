package it.pagopa.pnss.repositoryManager.dto;

public enum DocumentStateEnum {

	BOOKED("Booked"),
	STAGED("Staged"),
	VALID("Valid"),
	AVAILABLE("Available"),
	FREEZED("Freezed "),
	ATTACHED("Attached "),
	TODELETE("toDelete"),
	DELETED("Deleted ");
	
	private String value;
	
	private DocumentStateEnum(String value) {
		this.value = value;
	}
	
	public String value() {
		return this.value;
	}
}

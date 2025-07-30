package dk.digitalidentity.model.entity.data_processing.enums;

import lombok.Getter;

@Getter
public enum ReceiverLocation {
	INSIDE_EU("Indenfor EU"),
	OUTSIDE_EU("Udenfor EU");
	
	private String message;
	
	ReceiverLocation(String message) {
		this.message = message;
	}
}

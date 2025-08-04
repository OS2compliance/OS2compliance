package dk.digitalidentity.model.entity.data_processing.enums;

import lombok.Getter;

@Getter
public enum ReceiverLocation {
	INSIDE_EU("Beliggende indenfor EU"),
	OUTSIDE_EU("Beliggende udenfor EU");
	
	private String message;
	
	ReceiverLocation(String message) {
		this.message = message;
	}
}

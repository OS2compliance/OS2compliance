package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum AiRiskFactor {
	HIGH("Høj risiko"),
	LIMITED("Begrænset risiko"),
	MINIMAL("Minimal risiko");
	private final String message;

	AiRiskFactor(final String message) {
		this.message = message;
	}
}

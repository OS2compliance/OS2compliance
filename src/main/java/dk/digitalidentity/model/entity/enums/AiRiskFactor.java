package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum AiRiskFactor {
	MINIMAL("Minimal eller ingen risiko", "bg-green"),
	LIMITED("Begrænset risiko", "bg-orange"),
	HIGH("Høj risiko", "bg-red"),
	PROHIBITED("Forbudt/Uacceptabel risiko", "bg-danger");
	private final String message;
	private final String badgeClass;

	AiRiskFactor(final String message, final String badgeClass) {
		this.message = message;
		this.badgeClass = badgeClass;
	}
}

package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum ThirdCountryTransfer {
	YES("Ja"),
	NO("Nej"),
	UNDER_CLARIFICATION("Under afklaring");

	private final String message;

	ThirdCountryTransfer(final String message) {
		this.message = message;
	}
}

package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum ContainsAITechnologyEnum {
	YES("Ja"),
	NO("Nej"),
	UNDECIDED("Uafklaret");
	private final String message;

	ContainsAITechnologyEnum(final String message) {
		this.message = message;
	}
}

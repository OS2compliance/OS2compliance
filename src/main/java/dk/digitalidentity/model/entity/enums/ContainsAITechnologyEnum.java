package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum ContainsAITechnologyEnum {
	NO("No", "Nej"),
	YES("Yes", "Ja"),
	UNDECIDED("Undecided", "Uafklaret");
	private final String message;
	private final String danishName;

	ContainsAITechnologyEnum(final String message, final String danishName) {
		this.message = message;
		this.danishName = danishName;
	}
}

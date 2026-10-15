package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum AiAnswer {
	YES("Ja"),
	NO("Nej"),
	NEEDS_CLARIFICATION("Skal afklares"),
	NOT_RELEVANT("Ikke relevant");
	private final String message;

	AiAnswer(final String message) {
		this.message = message;
	}
}

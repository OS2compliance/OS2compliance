package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum DPIAScreeningConclusion {
	RED("Rød"),
	YELLOW("Gul"),
	GREEN("Grøn"),
	GREY("Grå");

	private final String message;

	DPIAScreeningConclusion(final String message) {
		this.message = message;
	}
}

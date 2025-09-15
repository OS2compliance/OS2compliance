package dk.digitalidentity.model.entity.enums;

import dk.digitalidentity.model.entity.interfaces.HasMessage;
import lombok.Getter;

@Getter
public enum DPIAScreeningConclusion implements HasMessage {
	RED("Rød"),
	YELLOW("Gul"),
	GREEN("Grøn"),
	GREY("Grå");

	private final String message;

	DPIAScreeningConclusion(final String message) {
		this.message = message;
	}
}

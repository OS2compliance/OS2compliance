package dk.digitalidentity.model.entity.enums;

import dk.digitalidentity.model.entity.interfaces.HasMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ThreatAssessmentCompletionStatus implements HasMessage {
	COMPLETED("Gennemført"),
	OPTED_OUT("Fravalgt"),
	PENDING("Ikke Gennemført");

	private final String message;
}

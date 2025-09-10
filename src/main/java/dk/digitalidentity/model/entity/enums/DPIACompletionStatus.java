package dk.digitalidentity.model.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
public enum DPIACompletionStatus {
	COMPLETED("Gennemført"),
	OPTED_OUT("Fravalgt"),
	PENDING("Ikke Gennemført");

	private final String message;
}

package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.enums.InformationObligationStatus;

import java.util.Set;

public record PurposeFormDTO (
		String purpose,
		Set<String> gdprChoices,
		InformationObligationStatus informationObligation,
		String informationObligationDesc,
		String consent,
		String purposeNotes,
		Set<String> relevantLegalReferences,
		String otherLegalReferences,
		String supplementalLegalBasis
) {}

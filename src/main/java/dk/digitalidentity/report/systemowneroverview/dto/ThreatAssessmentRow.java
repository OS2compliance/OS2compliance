package dk.digitalidentity.report.systemowneroverview.dto;

import dk.digitalidentity.model.dto.StatusCombination;

import java.time.LocalDateTime;

public record ThreatAssessmentRow(
		String name,
		String assetName,
		String type,
		String subjectArea,
		String riskOwner,
		LocalDateTime date,
		String status,
		StatusCombination riskAssessment
) {}

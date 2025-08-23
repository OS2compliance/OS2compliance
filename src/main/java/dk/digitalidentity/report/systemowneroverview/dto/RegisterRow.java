package dk.digitalidentity.report.systemowneroverview.dto;

import dk.digitalidentity.model.dto.StatusCombination;

import java.time.LocalDate;

public record RegisterRow(
		String name,
		String assetName,
		String responsibleOuName,
		LocalDate updatedAt,
		StatusCombination consequenceEstimate,
		StatusCombination status
){}
package dk.digitalidentity.report.systemowneroverview.dto;

import dk.digitalidentity.model.dto.StatusCombination;

import java.time.LocalDate;

public record AssetRow(
		String name,
		String supplier,
		String type,
		LocalDate updatedAt,
		StatusCombination riskAssessment,
		StatusCombination status
){}
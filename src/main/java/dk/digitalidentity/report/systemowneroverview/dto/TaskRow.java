package dk.digitalidentity.report.systemowneroverview.dto;

import dk.digitalidentity.model.dto.StatusCombination;

import java.time.LocalDate;

public record TaskRow(
		String name,
		String assetName,
		String type,
		String ouName,
		LocalDate deadline,
		String repeats,
		StatusCombination status,
		String tags
) {}
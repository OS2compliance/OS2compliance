package dk.digitalidentity.report.systemowneroverview.dto;

import dk.digitalidentity.model.dto.StatusCombination;

import java.time.LocalDate;

public record DocumentRow(String name, String assetName, String type, LocalDate nextRevision, StatusCombination status, String tags) {}
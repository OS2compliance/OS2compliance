package dk.digitalidentity.report.riskimage.dto;

import java.util.Map;

public record ThreatRow (
		String name,
		String description,
		String threatCatalogName,
		Map<String, Double> assetThreatScoresByThreatAssessmentName,
		Double totalThreatScore
){}

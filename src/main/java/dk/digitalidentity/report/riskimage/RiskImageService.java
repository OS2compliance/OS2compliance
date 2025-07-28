package dk.digitalidentity.report.riskimage;

import dk.digitalidentity.model.entity.CustomThreat;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import dk.digitalidentity.report.riskimage.dto.ThreatRow;
import dk.digitalidentity.service.ThreatAssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Service
public class RiskImageService {

	private final ThreatAssessmentService threatAssessmentService;

	public Set<ThreatAssessment> findRelevantThreatAssessments(List<String> includedTypes, List<String> latestOnlyTypes) {
		String assetType = "asset";
		String registerType = "register";
		String scenarioType = "scenario";

		Set<ThreatAssessmentType> getAllTypes = new HashSet<>();
		Set<ThreatAssessmentType> getLatestTypes = new HashSet<>();

		for (String type : includedTypes) {
			if (type.toLowerCase().trim().equals(assetType)) {
				boolean onlyLatestAsset = latestOnlyTypes != null && latestOnlyTypes.contains(assetType);
				if (onlyLatestAsset) {
					//find latest threatAssessments for each asset only
					getLatestTypes.add(ThreatAssessmentType.ASSET);
				}
				else {
					// Find all threatAssessments for all assets
					getAllTypes.add(ThreatAssessmentType.ASSET);
				}
			}
			else if (type.toLowerCase().trim().equals(registerType)) {
				boolean onlyLatestRegister = latestOnlyTypes != null && latestOnlyTypes.contains(registerType);
				if (onlyLatestRegister) {
					//find latest threatAssessments for each register only
					getLatestTypes.add(ThreatAssessmentType.REGISTER);
				}
				else {
					// Find all threatAssessments for all registers
					getAllTypes.add(ThreatAssessmentType.REGISTER);
				}
			}
			else if (type.toLowerCase().trim().equals(scenarioType)) {
				// Find all threatAssessments for all scenarios
				getAllTypes.add(ThreatAssessmentType.SCENARIO);
			}
		}

		Set<ThreatAssessment> threatAssessments = threatAssessmentService.findAllByTypes(getAllTypes);
		threatAssessments.addAll(findRelevantThreatAssessmentsByLatest(getLatestTypes));

		return threatAssessments;
	}

	private Set<ThreatAssessment> findRelevantThreatAssessmentsByLatest(Set<ThreatAssessmentType> getLatestTypes) {
		Set<ThreatAssessment> threatAssessments = new HashSet<>();
		for (ThreatAssessmentType type : getLatestTypes) {
			if (type == ThreatAssessmentType.REGISTER) {
				threatAssessments.addAll(threatAssessmentService.findLatestForAllRegisters());
			}
			else if (type == ThreatAssessmentType.ASSET) {
				threatAssessments.addAll(threatAssessmentService.findLatestForAllAssets());
			}
		}
		return threatAssessments;
	}

	public List<ThreatRow> mapToRows(Set<ThreatAssessment> threatAssessments) {
		// Map responses average score to assessment name
		Map<String, Double> threatAssessmentScoreMap = threatAssessments.stream()
				.collect(Collectors.toMap(Relatable::getName, t -> getAverageScore(t.getThreatAssessmentResponses())));

		// Construct maps of threatassessments for each threat
		Map<ThreatCatalogThreat, Set<ThreatAssessment>> threatAssessmentByThreatMap = new HashMap<>();
		Map<CustomThreat, Set<ThreatAssessment>> threatAssessmentByCustomThreatMap = new HashMap<>();

		for (ThreatAssessment assessment : threatAssessments) {
			List<ThreatCatalogThreat> threats = assessment.getThreatCatalog().getThreats();
			List<CustomThreat> customThreats = assessment.getCustomThreats();
			for (ThreatCatalogThreat threat : threats) {
				threatAssessmentByThreatMap.computeIfAbsent(threat, k -> new HashSet<>());
			}

			for (CustomThreat threat : customThreats) {
				threatAssessmentByCustomThreatMap.computeIfAbsent(threat, k -> new HashSet<>());
			}
		}

		// Construct ThreatRow DTO
		Stream<ThreatRow> threatRows = threatAssessmentByThreatMap.entrySet().stream()
				.map(entry -> fromThreatCatalogThreat(entry, threatAssessmentScoreMap));

		Stream<ThreatRow> customThreatRows = threatAssessmentByCustomThreatMap.entrySet().stream()
				.map(entry -> fromCustomThreat(entry, threatAssessmentScoreMap));

		// Return combined results
		return Stream.concat(threatRows, customThreatRows)
				.sorted(Comparator
						.comparing(ThreatRow::threatCatalogName)
						.thenComparing(ThreatRow::name))
				.toList();
	}

	private ThreatRow fromThreatCatalogThreat(Map.Entry<ThreatCatalogThreat, Set<ThreatAssessment>> entry, Map<String, Double> threatAssessmentScoreMap) {
		Set<String> threatAssessmenmtNames = entry.getValue().stream().map(ThreatAssessment::getName).collect(Collectors.toSet());
		Map<String, Double> assessmentScores = threatAssessmentScoreMap.entrySet().stream()
				.filter(e -> threatAssessmenmtNames.contains(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		return new ThreatRow(
				entry.getKey().getThreatType(),
				entry.getKey().getThreatCatalog().getName(),
				assessmentScores,
				assessmentScores.values().stream().mapToDouble(Double::valueOf).sum()
		);
	}

	private ThreatRow fromCustomThreat(Map.Entry<CustomThreat, Set<ThreatAssessment>> entry, Map<String, Double> threatAssessmentScoreMap) {
		Set<String> threatAssessmenmtNames = entry.getValue().stream().map(ThreatAssessment::getName).collect(Collectors.toSet());
		Map<String, Double> assessmentScores = threatAssessmentScoreMap.entrySet().stream()
				.filter(e -> threatAssessmenmtNames.contains(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		return new ThreatRow(
				entry.getKey().getThreatType(),
				"",
				assessmentScores,
				assessmentScores.values().stream().mapToDouble(Double::valueOf).sum()
		);
	}

	private Double getAverageScore(Collection<ThreatAssessmentResponse> threatAssessmentResponses) {
		return threatAssessmentResponses.stream()
				.map(tr -> {
					Integer probability = tr.getProbability();
					List<Integer> fieldValues = new ArrayList<>();
					fieldValues.add(tr.getAvailabilityRegistered());
					fieldValues.add(tr.getConfidentialityRegistered());
					fieldValues.add(tr.getIntegrityRegistered());
					fieldValues.add(tr.getAvailabilityOrganisation());
					fieldValues.add(tr.getConfidentialityOrganisation());
					fieldValues.add(tr.getIntegrityOrganisation());
					Integer max = fieldValues.stream().max(Integer::compare).get();
					return probability * max;
				})
				.mapToDouble(Double::valueOf)
				.average()
				.orElse(0);
	}
}

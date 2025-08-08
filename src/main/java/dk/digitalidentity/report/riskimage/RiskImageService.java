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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

	public Set<ThreatAssessment> findRelevantThreatAssessments(List<String> includedTypes, List<String> latestOnlyTypes, LocalDate startDate, LocalDate endDate) {
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

		Set<ThreatAssessment> threatAssessments = threatAssessmentService.findAllByTypesAndFromDateToDate(getAllTypes, startDate, endDate);
		threatAssessments.addAll(findRelevantThreatAssessmentsByLatest(getLatestTypes, startDate, endDate));

		return threatAssessments;
	}

	private Set<ThreatAssessment> findRelevantThreatAssessmentsByLatest(Set<ThreatAssessmentType> getLatestTypes, LocalDate startDate, LocalDate endDate) {
		Set<ThreatAssessment> threatAssessments = new HashSet<>();
		for (ThreatAssessmentType type : getLatestTypes) {
			if (type == ThreatAssessmentType.REGISTER) {
				threatAssessments.addAll(threatAssessmentService.findLatestForAllRegisters(startDate, endDate));
			}
			else if (type == ThreatAssessmentType.ASSET) {
				threatAssessments.addAll(threatAssessmentService.findLatestForAllAssets(startDate, endDate));
			}
		}
		return threatAssessments;
	}

	public List<ThreatRow> mapToRows(Set<ThreatAssessment> threatAssessments) {
		// Construct maps of threatassessments for each threat
		Map<ThreatCatalogThreat, Set<ThreatAssessment>> threatAssessmentByThreatMap = new HashMap<>();
		Map<CustomThreat, Set<ThreatAssessment>> threatAssessmentByCustomThreatMap = new HashMap<>();

		for (ThreatAssessment assessment : threatAssessments) {
			if (assessment.getThreatCatalog() != null) {
				List<ThreatCatalogThreat> threats = assessment.getThreatCatalog().getThreats();

				for (ThreatCatalogThreat threat : threats) {
					threatAssessmentByThreatMap.computeIfAbsent(threat, k -> new HashSet<>());
					threatAssessmentByThreatMap.get(threat).add(assessment);
				}
			}
			if (assessment.getCustomThreats() != null) {
				List<CustomThreat> customThreats = assessment.getCustomThreats();

				for (CustomThreat threat : customThreats) {
					threatAssessmentByCustomThreatMap.computeIfAbsent(threat, k -> new HashSet<>());
					threatAssessmentByCustomThreatMap.get(threat).add(assessment);
				}
			}
		}

		// Construct ThreatRow DTO
		Stream<ThreatRow> threatRows = threatAssessmentByThreatMap.entrySet().stream()
				.map(e -> fromThreatCatalogThreat(e.getKey(), e.getValue()));

		Stream<ThreatRow> customThreatRows = threatAssessmentByCustomThreatMap.entrySet().stream()
				.map(e -> fromCustomThreat(e.getKey(), e.getValue()));

		// Return combined results
		return Stream.concat(threatRows, customThreatRows)
				.sorted(Comparator.nullsLast(
						Comparator.comparing(ThreatRow::threatCatalogName)
								.thenComparing(ThreatRow::name)
								.thenComparing(ThreatRow::description)))
				.toList();
	}

	private ThreatRow fromThreatCatalogThreat(ThreatCatalogThreat threat, Set<ThreatAssessment> relevantAssessments) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM-yyyy HH:mm");
		Map<String, Double> assessmentScores = relevantAssessments.stream()
				.collect(Collectors.toMap(
						ta -> StringUtils.truncate(ta.getName(), 50) + " " + ta.getCreatedAt().format(formatter), // Truncated name of the threatassessment with date appended
						ta -> getAverageScore( // Get average score
								ta.getThreatAssessmentResponses().stream() // ...For responses of this treatassessment
										.filter(r -> r.getThreatCatalogThreat() != null && r.getThreatCatalogThreat().equals(threat)).toList() // ...That matches the relevant threat
						),
						(ta1, ta2) -> ta1));

		return new ThreatRow(
				threat.getThreatType(),
				threat.getDescription(),
				threat.getThreatCatalog() != null ? threat.getThreatCatalog().getName() : "",
				assessmentScores,
				assessmentScores.values().stream().mapToDouble(Double::valueOf).sum()
		);
	}

	private ThreatRow fromCustomThreat(CustomThreat threat, Set<ThreatAssessment> relevantAssessments) {
		Map<String, Double> assessmentScores = relevantAssessments.stream()
				.collect(Collectors.toMap(
						Relatable::getName,
						ta -> getAverageScore( // Get average score
								ta.getThreatAssessmentResponses().stream() // ...For responses of this treatassessment
										.filter(r -> r.getCustomThreat() != null && r.getCustomThreat().equals(threat)).toList()  // ...That matches the relevant threat
						),
						(ta1, ta2) -> ta1));

		return new ThreatRow(
				threat.getThreatType(),
				threat.getDescription(),
				"",
				assessmentScores,
				assessmentScores.values().stream().mapToDouble(Double::valueOf).sum()
		);
	}

	private Double getAverageScore(Collection<ThreatAssessmentResponse> threatAssessmentResponses) {
		return threatAssessmentResponses.stream()
				.map(tr -> {
					if (tr.getProbability() == null || tr.getProbability() < 1) {
						return 0;
					}
					Integer probability = tr.getProbability();
					List<Integer> fieldValues = new ArrayList<>();
					fieldValues.add(tr.getAvailabilityRegistered() == null ? 0 : tr.getAvailabilityRegistered());
					fieldValues.add(tr.getConfidentialityRegistered() == null ? 0 : tr.getConfidentialityRegistered());
					fieldValues.add(tr.getIntegrityRegistered() == null ? 0 : tr.getIntegrityRegistered());
					fieldValues.add(tr.getAvailabilityOrganisation() == null ? 0 : tr.getAvailabilityOrganisation());
					fieldValues.add(tr.getConfidentialityOrganisation() == null ? 0 : tr.getConfidentialityOrganisation());
					fieldValues.add(tr.getIntegrityOrganisation() == null ? 0 : tr.getIntegrityOrganisation());
					Integer max = fieldValues.stream().max(Integer::compare).get();
					return (probability * max);
				})
				.mapToInt(Integer::intValue)
				.average()
				.orElse(0.0);
	}
}

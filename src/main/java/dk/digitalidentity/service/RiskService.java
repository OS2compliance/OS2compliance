package dk.digitalidentity.service;

import dk.digitalidentity.dao.RegisterDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.dao.ThreatAssessmentDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.ConsequenceAssessment;
import dk.digitalidentity.model.entity.CustomThreat;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.RiskAssessment;
import dk.digitalidentity.model.entity.enums.ThreatDatabaseType;
import dk.digitalidentity.model.entity.enums.ThreatMethod;
import dk.digitalidentity.service.model.RiskDTO;
import dk.digitalidentity.service.model.RiskProfileDTO;
import dk.digitalidentity.service.model.ThreatDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class RiskService {

	@Autowired
	private RelationDao relationDao;
	@Autowired
	private RegisterDao registerDao;
    @Autowired
    private ScaleService scaleService;
    @Autowired
    private ThreatAssessmentDao threatAssessmentDao;

    public Optional<ThreatAssessment> findById(final Long assessmentId) {
        return threatAssessmentDao.findById(assessmentId);
    }

    public List<ThreatAssessment> findAll() {
        return threatAssessmentDao.findAll();
    }

	public RiskDTO calculateRiskFromRegisters(final Asset asset) {
		final List<Register> registers = relationDao.findRelatedToWithType(asset.getId(), RelationType.REGISTER).stream()
				.map(r -> r.getRelationAType() == RelationType.REGISTER ? r.getRelationAId() : r.getRelationBId())
				.map(rid -> registerDao.findById(rid).orElse(null))
				.filter(Objects::nonNull)
				.toList();

		int highestRF = 0;
		int highestOF = 0;
		int highestRI = 0;
		int highestOI = 0;
		int highestRT = 0;
		int highestOT = 0;

		for (final Register register : registers) {
			final ConsequenceAssessment consequenceAssessment = register.getConsequenceAssessment();
			if (consequenceAssessment == null) {
				continue;
			}

			if (consequenceAssessment.getConfidentialityRegistered() != null && consequenceAssessment.getConfidentialityRegistered() > highestRF) {
				highestRF = consequenceAssessment.getConfidentialityRegistered();
			}
			if (consequenceAssessment.getConfidentialityOrganisation() != null && consequenceAssessment.getConfidentialityOrganisation() > highestOF) {
				highestOF = consequenceAssessment.getConfidentialityOrganisation();
			}
			if (consequenceAssessment.getIntegrityRegistered() != null && consequenceAssessment.getIntegrityRegistered() > highestRI) {
				highestRI = consequenceAssessment.getIntegrityRegistered();
			}
			if (consequenceAssessment.getIntegrityOrganisation() != null && consequenceAssessment.getIntegrityOrganisation() > highestOI) {
				highestOI = consequenceAssessment.getIntegrityOrganisation();
			}
			if (consequenceAssessment.getAvailabilityRegistered() != null && consequenceAssessment.getAvailabilityRegistered() > highestRT) {
				highestRT = consequenceAssessment.getAvailabilityRegistered();
			}
			if (consequenceAssessment.getAvailabilityOrganisation() != null && consequenceAssessment.getAvailabilityOrganisation() > highestOT) {
				highestOT = consequenceAssessment.getAvailabilityOrganisation();
			}
		}

		return new RiskDTO(highestRF, highestOF, highestRI, highestOI, highestRT, highestOT);
	}

    public List<RiskProfileDTO> buildRiskProfileDTOs(final ThreatAssessment threatAssessment) {
        final List<RiskProfileDTO> riskProfiles = new ArrayList<>();
        final Map<String, List<ThreatDTO>> threatMap = buildThreatList(threatAssessment);
        for (final Map.Entry<String, List<ThreatDTO>> entry : threatMap.entrySet()) {
            for (final ThreatDTO threat: entry.getValue()) {
                final int highestConsequence = findHighestConsequence(threat);
                final int probability = threat.getProbability();

                if (probability < 1 || highestConsequence < 1) {
                    continue;
                }

                riskProfiles.add(new RiskProfileDTO(threat.getIndex(), highestConsequence, probability, threat.getResidualRiskConsequence(), threat.getResidualRiskProbability()));
            }
        }
        return riskProfiles;
    }

    public Map<String, List<ThreatDTO>> buildThreatList(final ThreatAssessment threatAssessment) {
        final Map<String, List<ThreatDTO>> threatMap = new LinkedHashMap<>();
        for (final ThreatCatalogThreat threat : threatAssessment.getThreatCatalog().getThreats()) {
            final ThreatAssessmentResponse response = threatAssessment.getThreatAssessmentResponses().stream()
                .filter(r -> r.getThreatCatalogThreat() != null && r.getThreatCatalogThreat().getIdentifier().equals(threat.getIdentifier()))
                .findAny().orElse(null);
            final ThreatDTO dto;
            if (response != null) {
                dto = new ThreatDTO(0, threat.getIdentifier(), ThreatDatabaseType.CATALOG, threat.getThreatType(), threat.getDescription(), response.isNotRelevant(), response.getProbability() != null ? response.getProbability() : -1, response.getConfidentialityRegistered() != null ? response.getConfidentialityRegistered() : -1, response.getIntegrityRegistered() != null ? response.getIntegrityRegistered() : -1, response.getAvailabilityRegistered() != null ? response.getAvailabilityRegistered() : -1, response.getConfidentialityOrganisation() != null ? response.getConfidentialityOrganisation() : -1, response.getIntegrityOrganisation() != null ? response.getIntegrityOrganisation() : -1, response.getAvailabilityOrganisation() != null ? response.getAvailabilityOrganisation() : -1, response.getProblem(), response.getExistingMeasures(), response.getMethod() == null ? ThreatMethod.NONE : response.getMethod(), response.getElaboration(), response.getResidualRiskConsequence() != null ? response.getResidualRiskConsequence() : -1, response.getResidualRiskProbability() != null ? response.getResidualRiskProbability() : -1);
            } else {
                dto = new ThreatDTO(0, threat.getIdentifier(), ThreatDatabaseType.CATALOG, threat.getThreatType(), threat.getDescription(), false, -1, -1, -1, -1, -1, -1, -1, null, null, ThreatMethod.NONE, null, -1, -1);
            }

            if (!threatMap.containsKey(threat.getThreatType())) {
                threatMap.put(threat.getThreatType(), new ArrayList<>());
            }
            threatMap.get(threat.getThreatType()).add(dto);
        }

        for (final CustomThreat threat : threatAssessment.getCustomThreats()) {
            final ThreatAssessmentResponse response = threatAssessment.getThreatAssessmentResponses().stream().filter(r -> r.getCustomThreat() != null && r.getCustomThreat().getId().equals(threat.getId())).findAny().orElse(null);
            final ThreatDTO dto;
            if (response != null) {
                dto = new ThreatDTO(threat.getId(), null, ThreatDatabaseType.CUSTOM, threat.getThreatType(), threat.getDescription(), response.isNotRelevant(), response.getProbability() != null ? response.getProbability() : -1, response.getConfidentialityRegistered() != null ? response.getConfidentialityRegistered() : -1, response.getIntegrityRegistered() != null ? response.getIntegrityRegistered() : -1, response.getAvailabilityRegistered() != null ? response.getAvailabilityRegistered() : -1, response.getConfidentialityOrganisation() != null ? response.getConfidentialityOrganisation() : -1, response.getIntegrityOrganisation() != null ? response.getIntegrityOrganisation() : -1, response.getAvailabilityOrganisation() != null ? response.getAvailabilityOrganisation() : -1, response.getProblem(), response.getExistingMeasures(), response.getMethod() == null ? ThreatMethod.NONE : response.getMethod(), response.getElaboration(), response.getResidualRiskConsequence() != null ? response.getResidualRiskConsequence() : -1, response.getResidualRiskProbability() != null ? response.getResidualRiskProbability() : -1);
            } else {
                dto = new ThreatDTO(threat.getId(), null, ThreatDatabaseType.CUSTOM, threat.getThreatType(), threat.getDescription(), false, -1, -1, -1, -1, -1, -1, -1, null, null, ThreatMethod.NONE, null, -1, -1);
            }

            if (!threatMap.containsKey(threat.getThreatType())) {
                threatMap.put(threat.getThreatType(), new ArrayList<>());
            }
            threatMap.get(threat.getThreatType()).add(dto);
        }

        int index = 0;
        for (final Map.Entry<String, List<ThreatDTO>> entry : threatMap.entrySet()) {
            for (final ThreatDTO threatDTO : entry.getValue()) {
                threatDTO.setIndex(index++);
            }
        }

        return threatMap;
    }

    private int findHighestConsequence(final ThreatDTO threat) {
        return findHighestConsequence(threat.getRf(), threat.getRi(), threat.getRt(), threat.getOf(), threat.getOi(), threat.getOt());
    }

    public void setThreatAssessmentColor(final ThreatAssessment savedThreatAssessment) {
        int highestRiskNotAcceptedRiskScore = -1;
        int globalHighestprobability = -1;
        int globalHighestConsequence = -1;
        for (final ThreatAssessmentResponse threatAssessmentResponse : savedThreatAssessment.getThreatAssessmentResponses()) {
            final int highestConsequence = findHighestConsequence(threatAssessmentResponse.getConfidentialityRegistered(), threatAssessmentResponse.getIntegrityRegistered(), threatAssessmentResponse.getAvailabilityRegistered(), threatAssessmentResponse.getConfidentialityOrganisation(), threatAssessmentResponse.getIntegrityOrganisation(), threatAssessmentResponse.getAvailabilityOrganisation());
            final int probability = threatAssessmentResponse.getProbability() == null ? 0 : threatAssessmentResponse.getProbability();

            if (probability < 1 || highestConsequence < 1) {
                continue;
            }
            final int riskScore = probability * highestConsequence;
            if (riskScore > highestRiskNotAcceptedRiskScore) {
                highestRiskNotAcceptedRiskScore = riskScore;
                globalHighestprobability = probability;
                globalHighestConsequence = highestConsequence;
            }
        }

        if (highestRiskNotAcceptedRiskScore != -1) {
            final Map<String, String> colorMap = scaleService.getScaleRiskScoreColorMap();
            final String color = colorMap.get(globalHighestConsequence + "," + globalHighestprobability);
            if ("GRØN".equals(color)) {
                savedThreatAssessment.setAssessment(RiskAssessment.GREEN);
            } else if ("GUL".equals(color)) {
                savedThreatAssessment.setAssessment(RiskAssessment.YELLOW);
            } else if ("RØD".equals(color)) {
                savedThreatAssessment.setAssessment(RiskAssessment.RED);
            } else {
                savedThreatAssessment.setAssessment(null);
            }
        } else {
            savedThreatAssessment.setAssessment(null);
        }

        threatAssessmentDao.save(savedThreatAssessment);
    }

    private int findHighestConsequence(final Integer rf, final Integer ri, final Integer rt, final Integer of, final Integer oi, final Integer ot) {
        int highest = 0;

        if (rf != null && rf > highest) {
            highest = rf;
        }
        if (ri != null && ri > highest) {
            highest = ri;
        }
        if (rt != null && rt > highest) {
            highest = rt;
        }
        if (of != null && of > highest) {
            highest = of;
        }
        if (oi != null && oi > highest) {
            highest = oi;
        }
        if (ot != null && ot > highest) {
            highest = ot;
        }

        return highest;
    }
}

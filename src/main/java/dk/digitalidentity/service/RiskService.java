package dk.digitalidentity.service;

import dk.digitalidentity.dao.RegisterDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.dao.ThreatAssessmentDao;
import dk.digitalidentity.model.entity.ConsequenceAssessment;
import dk.digitalidentity.model.entity.CustomThreat;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.RiskAssessment;
import dk.digitalidentity.model.entity.enums.ThreatDatabaseType;
import dk.digitalidentity.model.entity.enums.ThreatMethod;
import dk.digitalidentity.service.model.RiskDTO;
import dk.digitalidentity.service.model.RiskProfileDTO;
import dk.digitalidentity.service.model.TaskDTO;
import dk.digitalidentity.service.model.ThreatDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;

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
    @Autowired
    private RelationService relationService;

    public Optional<ThreatAssessment> findById(final Long assessmentId) {
        return threatAssessmentDao.findById(assessmentId);
    }

    public List<ThreatAssessment> findAll() {
        return threatAssessmentDao.findAll();
    }

	public RiskDTO calculateRiskFromRegisters(final List<Long> assetIds) {
		final List<Register> registers = relationDao.findRelatedToWithType(assetIds, RelationType.REGISTER).stream()
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
                dto = new ThreatDTO(0, response.getId(), threat.getIdentifier(), ThreatDatabaseType.CATALOG, threat.getThreatType(), threat.getDescription(), response.isNotRelevant(), response.getProbability() != null ? response.getProbability() : -1, response.getConfidentialityRegistered() != null ? response.getConfidentialityRegistered() : -1, response.getIntegrityRegistered() != null ? response.getIntegrityRegistered() : -1, response.getAvailabilityRegistered() != null ? response.getAvailabilityRegistered() : -1, response.getConfidentialityOrganisation() != null ? response.getConfidentialityOrganisation() : -1, response.getIntegrityOrganisation() != null ? response.getIntegrityOrganisation() : -1, response.getAvailabilityOrganisation() != null ? response.getAvailabilityOrganisation() : -1, response.getProblem(), response.getExistingMeasures(), response.getMethod() == null ? ThreatMethod.NONE : response.getMethod(), response.getElaboration(), response.getResidualRiskConsequence() != null ? response.getResidualRiskConsequence() : -1, response.getResidualRiskProbability() != null ? response.getResidualRiskProbability() : -1);
                addRelatedTasks(response, dto);
            } else {
                dto = new ThreatDTO(0, 0, threat.getIdentifier(), ThreatDatabaseType.CATALOG, threat.getThreatType(), threat.getDescription(), false, -1, -1, -1, -1, -1, -1, -1, null, null, ThreatMethod.NONE, null, -1, -1);
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

        for (final CustomThreat threat : threatAssessment.getCustomThreats()) {
            final ThreatAssessmentResponse response = threatAssessment.getThreatAssessmentResponses().stream().filter(r -> r.getCustomThreat() != null && r.getCustomThreat().getId().equals(threat.getId())).findAny().orElse(null);
            final ThreatDTO dto;
            if (response != null) {
                dto = new ThreatDTO(threat.getId(), response.getId(), null, ThreatDatabaseType.CUSTOM, threat.getThreatType(), threat.getDescription(), response.isNotRelevant(), response.getProbability() != null ? response.getProbability() : -1, response.getConfidentialityRegistered() != null ? response.getConfidentialityRegistered() : -1, response.getIntegrityRegistered() != null ? response.getIntegrityRegistered() : -1, response.getAvailabilityRegistered() != null ? response.getAvailabilityRegistered() : -1, response.getConfidentialityOrganisation() != null ? response.getConfidentialityOrganisation() : -1, response.getIntegrityOrganisation() != null ? response.getIntegrityOrganisation() : -1, response.getAvailabilityOrganisation() != null ? response.getAvailabilityOrganisation() : -1, response.getProblem(), response.getExistingMeasures(), response.getMethod() == null ? ThreatMethod.NONE : response.getMethod(), response.getElaboration(), response.getResidualRiskConsequence() != null ? response.getResidualRiskConsequence() : -1, response.getResidualRiskProbability() != null ? response.getResidualRiskProbability() : -1);
                dto.setIndex(index++);
                addRelatedTasks(response, dto);
            } else {
                dto = new ThreatDTO(threat.getId(), 0, null, ThreatDatabaseType.CUSTOM, threat.getThreatType(), threat.getDescription(), false, -1, -1, -1, -1, -1, -1, -1, null, null, ThreatMethod.NONE, null, -1, -1);
                dto.setIndex(index++);
            }

            if (!threatMap.containsKey(threat.getThreatType())) {
                threatMap.put(threat.getThreatType(), new ArrayList<>());
            }
            threatMap.get(threat.getThreatType()).add(dto);
        }



        return threatMap;
    }

    private void addRelatedTasks(ThreatAssessmentResponse response, ThreatDTO dto) {
        final List<Task> relatedTasks = relationService.findAllRelatedTo(response).stream().filter(r -> r.getRelationType() == RelationType.TASK).map(r -> (Task) r).toList();
        List<TaskDTO> taskDTOS = new ArrayList<>();
        for (Task relatedTask : relatedTasks) {
            taskDTOS.add(new TaskDTO(relatedTask.getId(), relatedTask.getName(), relatedTask.getTaskType(), relatedTask.getResponsibleUser().getName(), relatedTask.getNextDeadline().format(DK_DATE_FORMATTER), relatedTask.getNextDeadline().isBefore(LocalDate.now())));
        }
        dto.setTasks(taskDTOS);
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

    public ThreatAssessmentResponse createResponse(final ThreatAssessment threatAssessment, final ThreatCatalogThreat threatCatalogThreat, final CustomThreat customThreat) {
        final ThreatAssessmentResponse response = new ThreatAssessmentResponse();
        if (customThreat != null) {
            response.setName(customThreat.getDescription());
        } else if (threatCatalogThreat != null) {
            response.setName(threatCatalogThreat.getDescription());
        }
        response.setMethod(ThreatMethod.NONE);
        response.setThreatAssessment(threatAssessment);
        response.setCustomThreat(customThreat);
        response.setThreatCatalogThreat(threatCatalogThreat);
        threatAssessment.getThreatAssessmentResponses().add(response);
        return response;
    }
}

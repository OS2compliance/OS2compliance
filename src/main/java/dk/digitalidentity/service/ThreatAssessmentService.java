package dk.digitalidentity.service;

import dk.digitalidentity.dao.RegisterDao;
import dk.digitalidentity.dao.ThreatAssessmentDao;
import dk.digitalidentity.model.entity.ConsequenceAssessment;
import dk.digitalidentity.model.entity.CustomThreat;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.RiskAssessment;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.enums.ThreatDatabaseType;
import dk.digitalidentity.model.entity.enums.ThreatMethod;
import dk.digitalidentity.service.model.RiskDTO;
import dk.digitalidentity.service.model.RiskProfileDTO;
import dk.digitalidentity.service.model.ThreatDTO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThreatAssessmentService {
	private final RelationService relationService;
    private final RegisterDao registerDao;
    private final ScaleService scaleService;
    private final ThreatAssessmentDao threatAssessmentDao;
    private final TaskService taskService;

    public Optional<ThreatAssessment> findById(final Long assessmentId) {
        return threatAssessmentDao.findById(assessmentId);
    }

    public List<ThreatAssessment> findAll() {
        return threatAssessmentDao.findAll();
    }

    @Transactional
    public ThreatAssessment save(final ThreatAssessment assessment) {
        return threatAssessmentDao.save(assessment);
    }

    public ThreatAssessment copy(final long sourceId) {
        final ThreatAssessment sourceAssessment = threatAssessmentDao.findById(sourceId).orElseThrow();
        final ThreatAssessment targetAssessment = new ThreatAssessment();
        targetAssessment.setName(sourceAssessment.getName());
        targetAssessment.setThreatAssessmentType(sourceAssessment.getThreatAssessmentType());
        targetAssessment.setResponsibleUser(sourceAssessment.getResponsibleUser());
        targetAssessment.setResponsibleOu(sourceAssessment.getResponsibleOu());
        targetAssessment.setThreatCatalog(sourceAssessment.getThreatCatalog());
        targetAssessment.setRegistered(sourceAssessment.isRegistered());
        targetAssessment.setOrganisation(sourceAssessment.isOrganisation());
        targetAssessment.setInherit(sourceAssessment.isInherit());

        targetAssessment.setInheritedConfidentialityRegistered(sourceAssessment.getInheritedConfidentialityRegistered());
        targetAssessment.setInheritedConfidentialityOrganisation(sourceAssessment.getInheritedConfidentialityOrganisation());
        targetAssessment.setInheritedIntegrityRegistered(sourceAssessment.getInheritedIntegrityRegistered());
        targetAssessment.setInheritedIntegrityOrganisation(sourceAssessment.getInheritedIntegrityOrganisation());
        targetAssessment.setInheritedAvailabilityRegistered(sourceAssessment.getInheritedAvailabilityRegistered());
        targetAssessment.setInheritedAvailabilityOrganisation(sourceAssessment.getInheritedAvailabilityOrganisation());
        targetAssessment.setAssessment(sourceAssessment.getAssessment());
        final ThreatAssessment savedAssessment = threatAssessmentDao.save(targetAssessment);

        final List<CustomThreat> customThreats = sourceAssessment.getCustomThreats().stream()
            .map(c -> copyCustomThreat(savedAssessment, c))
            .toList();
        savedAssessment.setCustomThreats(customThreats);
        final List<ThreatAssessmentResponse> responses = sourceAssessment.getThreatAssessmentResponses().stream()
            .map(r -> copyResponse(savedAssessment, customThreats, r))
            .collect(Collectors.toList());
        savedAssessment.setThreatAssessmentResponses(responses);
        return savedAssessment;
    }

    private CustomThreat copyCustomThreat(final ThreatAssessment assessment, final CustomThreat c) {
        final CustomThreat target = new CustomThreat();
        target.setThreatAssessment(assessment);
        target.setThreatType(c.getThreatType());
        target.setDescription(c.getDescription());
        return target;
    }

    private static ThreatAssessmentResponse copyResponse(final ThreatAssessment assessment, final List<CustomThreat> customThreats,
                                                         final ThreatAssessmentResponse sourceResponse) {
        final ThreatAssessmentResponse t = new ThreatAssessmentResponse();
        t.setNotRelevant(sourceResponse.isNotRelevant());
        t.setProbability(sourceResponse.getProbability());
        t.setConfidentialityRegistered(sourceResponse.getConfidentialityRegistered());
        t.setConfidentialityOrganisation(sourceResponse.getConfidentialityOrganisation());
        t.setIntegrityRegistered(sourceResponse.getIntegrityRegistered());
        t.setIntegrityOrganisation(sourceResponse.getIntegrityOrganisation());
        t.setAvailabilityRegistered(sourceResponse.getAvailabilityRegistered());
        t.setAvailabilityOrganisation(sourceResponse.getAvailabilityOrganisation());
        t.setProblem(sourceResponse.getProblem());
        t.setExistingMeasures(sourceResponse.getExistingMeasures());
        t.setMethod(sourceResponse.getMethod());
        t.setElaboration(sourceResponse.getElaboration());
        t.setResidualRiskConsequence(sourceResponse.getResidualRiskConsequence());
        t.setResidualRiskProbability(sourceResponse.getResidualRiskProbability());
        t.setThreatAssessment(assessment);
        t.setThreatCatalogThreat(sourceResponse.getThreatCatalogThreat());
        final CustomThreat sourceCustomThreat = sourceResponse.getCustomThreat();
        if (sourceCustomThreat != null) {
            final CustomThreat customThreat = customThreats.stream()
                .filter(c -> StringUtils.equals(c.getThreatType(), sourceCustomThreat.getThreatType()) &&
                    StringUtils.equals(c.getDescription(), sourceCustomThreat.getDescription()))
                .findFirst().orElse(null);
            t.setCustomThreat(customThreat);
        }
        return t;
    }

    @Transactional
    public void deleteById(final Long threatAssessmentId) {
        threatAssessmentDao.deleteById(threatAssessmentId);
    }

    public Task createAssociatedTask(final ThreatAssessment assessment) {
        if (assessment.getResponsibleUser() != null) {
            Task task = new Task();
            task.setName("Udfyld risikovurdering: " + assessment.getName());
            task.setTaskType(TaskType.TASK);
            task.setResponsibleUser(assessment.getResponsibleUser());
            task.setNextDeadline(LocalDate.now().plusMonths(1));
            task.setRepetition(TaskRepetition.NONE);
            task = taskService.createTask(task);

            relationService.addRelation(assessment, task);
            return task;
        }
        return null;
    }
	public RiskDTO calculateRiskFromRegisters(final List<Long> assetIds) {
		final List<Register> registers = relationService.findRelatedToWithType(assetIds, RelationType.REGISTER).stream()
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
                dto = new ThreatDTO(threat.getId(), null, ThreatDatabaseType.CUSTOM, threat.getThreatType(), threat.getDescription(), response.isNotRelevant(), response.getProbability() != null ? response.getProbability() : -1, response.getConfidentialityRegistered() != null ? response.getConfidentialityRegistered() : -1, response.getIntegrityRegistered() != null ? response.getIntegrityRegistered() : -1, response.getAvailabilityRegistered() != null ? response.getAvailabilityRegistered() : -1, response.getConfidentialityOrganisation() != null ? response.getConfidentialityOrganisation() : -1, response.getIntegrityOrganisation() != null ? response.getIntegrityOrganisation() : -1, response.getAvailabilityOrganisation() != null ? response.getAvailabilityOrganisation() : -1, response.getProblem(), response.getExistingMeasures(), response.getMethod() == null ? ThreatMethod.NONE : response.getMethod(), response.getElaboration(), response.getResidualRiskConsequence() != null ? response.getResidualRiskConsequence() : -1, response.getResidualRiskProbability() != null ? response.getResidualRiskProbability() : -1);
                dto.setIndex(index++);
            } else {
                dto = new ThreatDTO(threat.getId(), null, ThreatDatabaseType.CUSTOM, threat.getThreatType(), threat.getDescription(), false, -1, -1, -1, -1, -1, -1, -1, null, null, ThreatMethod.NONE, null, -1, -1);
                dto.setIndex(index++);
            }

            if (!threatMap.containsKey(threat.getThreatType())) {
                threatMap.put(threat.getThreatType(), new ArrayList<>());
            }
            threatMap.get(threat.getThreatType()).add(dto);
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

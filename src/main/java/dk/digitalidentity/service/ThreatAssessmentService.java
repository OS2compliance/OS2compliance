package dk.digitalidentity.service;

import dk.digitalidentity.dao.RegisterDao;
import dk.digitalidentity.dao.ThreatAssessmentDao;
import dk.digitalidentity.dao.ThreatAssessmentResponseDao;
import dk.digitalidentity.model.dto.RegisterAssetRiskDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.ConsequenceAssessment;
import dk.digitalidentity.model.entity.CustomThreat;
import dk.digitalidentity.model.entity.Precaution;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.S3Document;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.RiskAssessment;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import dk.digitalidentity.model.entity.enums.ThreatDatabaseType;
import dk.digitalidentity.model.entity.enums.ThreatMethod;
import dk.digitalidentity.service.model.RiskDTO;
import dk.digitalidentity.service.model.RiskProfileDTO;
import dk.digitalidentity.service.model.TaskDTO;
import dk.digitalidentity.service.model.ThreatDTO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.ASSOCIATED_THREAT_ASSESSMENT_PROPERTY;
import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;
import static dk.digitalidentity.util.NullSafe.nullSafe;

@Service
@RequiredArgsConstructor
public class ThreatAssessmentService {
	private final RelationService relationService;
    private final RegisterDao registerDao;
    private final ScaleService scaleService;
    private final ThreatAssessmentDao threatAssessmentDao;
    private final TaskService taskService;
    private final UserService userService;
    private final TemplateEngine templateEngine;
    private final ChoiceService choiceService;
	private final ThreatAssessmentResponseDao threatAssessmentResponseDao;

	public ThreatAssessment findByS3Document(S3Document s3Document) {
        return threatAssessmentDao.findByThreatAssessmentReportS3DocumentId(s3Document.getId());
    }
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
        t.setName(sourceResponse.getName());
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

    public Optional<Task> findAssociatedCheck(final ThreatAssessment assessment) {
        final List<Task> tasks = taskService.findTaskWithProperty(ASSOCIATED_THREAT_ASSESSMENT_PROPERTY, "" + assessment.getId());
        if (tasks == null || tasks.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(tasks.get(0));
    }

    @Transactional
    public void updateNextRevisionAssociatedTask(final ThreatAssessment assessment) {
        findAssociatedCheck(assessment)
            .ifPresent(t -> assessment.setNextRevision(t.getNextDeadline()));
    }

    @Transactional
    public Task createOrUpdateAssociatedCheck(final ThreatAssessment assessment) {
        final LocalDate deadline = assessment.getNextRevision();
        if (deadline != null && assessment.getRevisionInterval() != null) {
            final Task task = findAssociatedCheck(assessment).orElseGet(() -> createAssociatedCheck(assessment));
            task.setName("Risikovurdering af " + assessment.getName());
            task.setResponsibleUser(assessment.getResponsibleUser());
            task.setNextDeadline(assessment.getNextRevision());
            task.setResponsibleUser(assessment.getResponsibleUser() != null ? assessment.getResponsibleUser() : userService.currentUser());
            task.setDescription("Revider risikovurdering af " + assessment.getName());
            setTaskRevisionInterval(assessment, task);
            return task;
        }
        return null;
    }

    private Task createAssociatedCheck(final ThreatAssessment assessment) {
        final Task task = new Task();
        task.setName("Revision af " + assessment.getName());
        task.setCreatedAt(LocalDateTime.now());
        task.getProperties().add(Property.builder()
            .entity(task)
            .key(ASSOCIATED_THREAT_ASSESSMENT_PROPERTY)
            .value("" + assessment.getId())
            .build()
        );
        task.setTaskType(TaskType.CHECK);
        task.setResponsibleUser(assessment.getResponsibleUser());
        task.setNextDeadline(assessment.getNextRevision());
        task.setNotifyResponsible(true);
        final Task savedTask = taskService.saveTask(task);
        relationService.addRelation(assessment, task); // TODO Dont re-add
        return savedTask;
    }

    @Transactional
    public Task createAssociatedTask(final ThreatAssessment assessment) {
        if (assessment.getResponsibleUser() != null) {
            Task task = new Task();
            task.setName("Udfyld risikovurdering: " + assessment.getName());
            task.setTaskType(TaskType.TASK);
            task.setResponsibleUser(assessment.getResponsibleUser());
            task.setNextDeadline(LocalDate.now().plusMonths(1));
            task.setRepetition(TaskRepetition.NONE);
            task = taskService.saveTask(task);

            relationService.addRelation(assessment, task);
            return task;
        }
        return null;
    }

    /**
     * Calculates the risk for assets related to a register.
     *
     * @param asset       The asset to calculate the risk for.
     * @param riskScale   The risk scale to be applied in the calculation.
     * @return An Optional containing the calculated RegisterAssetRiskDTO if a threat assessment is found for the asset,
     *         otherwise an empty Optional.
     */
    @Transactional
    public Optional<RegisterAssetRiskDTO> calculateRiskForRegistersRelatedAssets(final Asset asset, final Integer riskScale) {
        final List<Relation> relatedToWithType = relationService.findRelatedToWithType(asset, RelationType.THREAT_ASSESSMENT);
        return relatedToWithType.stream()
            .map(relation -> threatAssessmentDao.findById(relation.getRelationAType() == RelationType.THREAT_ASSESSMENT
                ? relation.getRelationAId() : relation.getRelationBId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .max(Comparator.comparing(ThreatAssessment::getCreatedAt))
            .map(threatAssessment -> {
                final List<RiskProfileDTO> riskProfileDTOs = buildRiskProfileDTOs(threatAssessment);
                final Pair<Integer, Integer> highestRiskScore = findHighestRiskScore(riskProfileDTOs);
                final double riskProbability = highestRiskScore.getLeft();
                final double riskConsequence = highestRiskScore.getRight();
                final double weight = (riskScale != null && riskScale > 0) ? riskScale / 100.0 : 0;
                final RegisterAssetRiskDTO registerAssetRiskDTO = new RegisterAssetRiskDTO();
                registerAssetRiskDTO.setThreatAssessment(threatAssessment);
                registerAssetRiskDTO.setProbability((int)Math.ceil(riskProbability));
                registerAssetRiskDTO.setConsequence((int)Math.ceil(riskConsequence));
                registerAssetRiskDTO.setDate(registerAssetRiskDTO.getDate());
                registerAssetRiskDTO.setRiskScore(riskProbability * riskConsequence);
                registerAssetRiskDTO.setWeightedPct(riskScale);
                registerAssetRiskDTO.setWeightedConsequence(riskConsequence * weight);
                registerAssetRiskDTO.setWeightedRiskScore((riskConsequence * weight) * riskProbability);
                final RiskAssessment weightedAssessment =
                    scaleService.getRiskAssessmentForRisk((int)Math.ceil(riskProbability), (int)Math.ceil(riskConsequence * weight));
                registerAssetRiskDTO.setWeightedAssessment(weightedAssessment);
                return registerAssetRiskDTO;
            });
    }

    /**
     * Find the highest risk score based on a list of RiskProfileDTO objects.
     * @param riskProfileDTOs The list of RiskProfileDTO objects containing the risk profile information.
     */
    private Pair<Integer, Integer> findHighestRiskScore(final List<RiskProfileDTO> riskProfileDTOs) {
        return riskProfileDTOs.stream()
            .max(Comparator.comparing(r -> r.getConsequence() * r.getProbability()))
            .map(r -> Pair.of(r.getProbability(), r.getConsequence()))
            .orElse(Pair.of(0, 0));
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

    @Transactional
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
                final List<Relatable> relatedPrecautions = relationService.findAllRelatedTo(response).stream().filter(r -> r.getRelationType().equals(RelationType.PRECAUTION)).collect(Collectors.toList());
                dto = new ThreatDTO(0, response.getId(), threat.getIdentifier(), ThreatDatabaseType.CATALOG, threat.getThreatType(), threat.getDescription(), response.isNotRelevant(), response.getProbability() != null ? response.getProbability() : -1, response.getConfidentialityRegistered() != null ? response.getConfidentialityRegistered() : -1, response.getIntegrityRegistered() != null ? response.getIntegrityRegistered() : -1, response.getAvailabilityRegistered() != null ? response.getAvailabilityRegistered() : -1, response.getConfidentialityOrganisation() != null ? response.getConfidentialityOrganisation() : -1, response.getIntegrityOrganisation() != null ? response.getIntegrityOrganisation() : -1, response.getAvailabilityOrganisation() != null ? response.getAvailabilityOrganisation() : -1, response.getProblem(), response.getExistingMeasures(), relatedPrecautions, response.getMethod() == null ? ThreatMethod.NONE : response.getMethod(), response.getElaboration(), response.getResidualRiskConsequence() != null ? response.getResidualRiskConsequence() : -1, response.getResidualRiskProbability() != null ? response.getResidualRiskProbability() : -1);
                addRelatedTasks(response, dto);
            } else {
                dto = new ThreatDTO(0, 0, threat.getIdentifier(), ThreatDatabaseType.CATALOG, threat.getThreatType(), threat.getDescription(), false, -1, -1, -1, -1, -1, -1, -1, null, null, new ArrayList<>(), ThreatMethod.NONE, null, -1, -1);
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
                final List<Relatable> relatedPrecautions = relationService.findAllRelatedTo(response).stream().filter(r -> r.getRelationType().equals(RelationType.PRECAUTION)).collect(Collectors.toList());
                dto = new ThreatDTO(threat.getId(), response.getId(), null, ThreatDatabaseType.CUSTOM, threat.getThreatType(), threat.getDescription(), response.isNotRelevant(), response.getProbability() != null ? response.getProbability() : -1, response.getConfidentialityRegistered() != null ? response.getConfidentialityRegistered() : -1, response.getIntegrityRegistered() != null ? response.getIntegrityRegistered() : -1, response.getAvailabilityRegistered() != null ? response.getAvailabilityRegistered() : -1, response.getConfidentialityOrganisation() != null ? response.getConfidentialityOrganisation() : -1, response.getIntegrityOrganisation() != null ? response.getIntegrityOrganisation() : -1, response.getAvailabilityOrganisation() != null ? response.getAvailabilityOrganisation() : -1, response.getProblem(), response.getExistingMeasures(), relatedPrecautions, response.getMethod() == null ? ThreatMethod.NONE : response.getMethod(), response.getElaboration(), response.getResidualRiskConsequence() != null ? response.getResidualRiskConsequence() : -1, response.getResidualRiskProbability() != null ? response.getResidualRiskProbability() : -1);
                dto.setIndex(index++);
                addRelatedTasks(response, dto);
            } else {
                dto = new ThreatDTO(threat.getId(), 0, null, ThreatDatabaseType.CUSTOM, threat.getThreatType(), threat.getDescription(), false, -1, -1, -1, -1, -1, -1, -1, null, null, new ArrayList<>(), ThreatMethod.NONE, null, -1, -1);
                dto.setIndex(index++);
            }

            if (!threatMap.containsKey(threat.getThreatType())) {
                threatMap.put(threat.getThreatType(), new ArrayList<>());
            }
            threatMap.get(threat.getThreatType()).add(dto);
        }



        return threatMap;
    }

    private void addRelatedTasks(final ThreatAssessmentResponse response, final ThreatDTO dto) {
        final List<Task> relatedTasks = relationService.findAllRelatedTo(response).stream().filter(r -> r.getRelationType() == RelationType.TASK).map(r -> (Task) r).toList();
        final List<TaskDTO> taskDTOS = new ArrayList<>();
        for (final Task relatedTask : relatedTasks) {
            taskDTOS.add(new TaskDTO(relatedTask.getId(), relatedTask.getName(), relatedTask.getTaskType(), relatedTask.getResponsibleUser().getName(), relatedTask.getNextDeadline().format(DK_DATE_FORMATTER), relatedTask.getNextDeadline().isBefore(LocalDate.now()), taskService.findHtmlStatusBadgeForTask(relatedTask)));
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
            final RiskAssessment assessment =
                scaleService.getRiskAssessmentForRisk(globalHighestprobability, globalHighestConsequence);
            savedThreatAssessment.setAssessment(assessment);
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
        return threatAssessmentResponseDao.save(response);
    }

    public byte[] getThreatAssessmentPdf(ThreatAssessment threatAssessment) throws IOException {
        var html = getThreatAssessmentHtml(threatAssessment);
        return convertHtmlToPdf(html);
    }

    public record registeredDataCategory (String title, List<String> types) {}
    private String getThreatAssessmentHtml(ThreatAssessment threatAssessment) {
        final List<Relatable> relations = relationService.findAllRelatedTo(threatAssessment);
        List<Task> riskAssessmentTasks = relations.stream().filter(t -> t.getRelationType() == RelationType.TASK)
            .map(Task.class::cast)
            .collect(Collectors.toList());
        List<Task> otherTasks = new ArrayList<>();
        Asset riskAsset = null;
        Register riskRegister = null;
        final List<Long> riskAssessmentTasksIds = riskAssessmentTasks.stream().map(Relatable::getId).toList();
        if (ThreatAssessmentType.ASSET == threatAssessment.getThreatAssessmentType()) {
            final Optional<Asset> asset = relations.stream().filter(r -> r.getRelationType() == RelationType.ASSET)
                .map(Asset.class::cast)
                .findFirst();
            if (asset.isPresent()) {
                riskAsset = asset.get();
                otherTasks = relationService.findAllRelatedTo(riskAsset).stream()
                    .filter(related -> related.getRelationType() == RelationType.TASK)
                    .filter(related -> !riskAssessmentTasksIds.contains(related.getId()))
                    .map(Task.class::cast)
                    .filter(t -> !taskService.isTaskDone(t))
                    .collect(Collectors.toList());
            }
        } else if (ThreatAssessmentType.REGISTER == threatAssessment.getThreatAssessmentType()) {
            final Optional<Register> register = relations.stream().filter(r -> r.getRelationType() == RelationType.REGISTER)
                .map(Register.class::cast)
                .findFirst();
            if (register.isPresent()) {
                riskRegister = register.get();
                otherTasks = relationService.findAllRelatedTo(riskRegister).stream()
                    .filter(related -> related.getRelationType() == RelationType.TASK)
                    .filter(related -> !riskAssessmentTasksIds.contains(related.getId()))
                    .map(Task.class::cast)
                    .filter(t -> !taskService.isTaskDone(t))
                    .collect(Collectors.toList());
            }
        }

        var context = new Context();
        context.setVariable("title", threatAssessment.getName());
        context.setVariable("subHeader", getSubHeading(threatAssessment, riskAsset, riskRegister));
        context.setVariable("present", getPresent(threatAssessment));
        context.setVariable("criticality", getCriticality(riskAsset, riskRegister));

        // asset / register info
        context = addGeneralInfoToContext(context, riskAsset, riskRegister);

        // risk profile
        context.setVariable("reversedScale", scaleService.getConsequenceScale().keySet().stream().sorted(Collections.reverseOrder()).collect(Collectors.toList()));
        List<RiskProfileDTO> riskProfiles = buildRiskProfileDTOs(threatAssessment);
        context.setVariable("riskProfiles", riskProfiles);
        final Map<String, String> colorMap = scaleService.getScaleRiskScoreColorMap();
        context.setVariable("riskScoreColorMap", scaleService.getScaleRiskScoreColorMap());
        context.setVariable("riskProfilesValueMap", buildRiskProfileValueMap(riskProfiles));
        context.setVariable("riskProfilesValueMapAfter", buildRiskProfileValueMapAfter(riskProfiles));

        // scale explainers
        final ScaleService.ScaleSetting scaleExplainers = ScaleService.scaleSettingsForType(scaleService.getScaleType());
        context.setVariable("probabilityScore", scaleExplainers.getProbabilityScore());
        context.setVariable("consequenceNumber", scaleExplainers.getConsequenceNumber());
        context.setVariable("riskScore", scaleExplainers.getRiskScore());

        // threat list
        Map<String, List<ThreatDTO>> threatList = buildThreatList(threatAssessment);
        context.setVariable("threatsForPDF", buildThreatsForPDF(threatList, riskProfiles, colorMap));

        // taskLists
        context.setVariable("tasksForPDF", buildTasks(riskAssessmentTasks));
        context.setVariable("otherTasksForPDF", buildTasks(otherTasks));


        return templateEngine.process("reports/risk_view_pdf", context);
    }

    public record PrecautionDTO (String name, String description) {}
    private List<PrecautionDTO> buildPrecautions (List<Precaution> precautions) {
        return precautions.stream().map(precaution ->
                new PrecautionDTO(precaution.getName(), precaution  .getDescription()))
            .toList();
    }

    private Context addGeneralInfoToContext (Context context, Asset riskAsset, Register riskRegister) {
        if (riskAsset != null) {
            context.setVariable("systemType", riskAsset.getAssetType().getCaption());
            String systemOwners = riskAsset.getResponsibleUsers().stream().map(User::getName).collect(Collectors.joining(", "));
            context.setVariable("systemOwners", systemOwners.isBlank() ? "Ikke udfyldt" : systemOwners);
            context.setVariable("supplier", riskAsset.getSupplier() != null ?  riskAsset.getSupplier().getName() : "Ukendt");
            context.setVariable("systemResponsible", riskAsset.getManagers().stream().map(User::getName).collect(Collectors.joining(", ")));
            context.setVariable("deletionProcedureCreated", riskAsset.getDataProcessing().getDeletionProcedure() != null ? riskAsset.getDataProcessing().getDeletionProcedure().getMessage() : "Ikke udfyldt");
            context.setVariable("deletionProcedureLink", riskAsset.getDataProcessing().getDeletionProcedureLink());
            String dataAccessPersons = riskAsset.getDataProcessing().getAccessWhoIdentifiers().stream()
                .map(identifier ->
                {
                    Optional<ChoiceValue> value = choiceService.getValue(identifier);
                    if (value.isPresent()) {
                        return value.get().getCaption();
                    }
                    return "Ikke udfyldt";
                }).collect(Collectors.joining(", "));
            context.setVariable("dataAccessPersons", dataAccessPersons.isBlank() ? "Ikke udfyldt" : dataAccessPersons );

            var accessCount = choiceService.getValue(riskAsset.getDataProcessing().getAccessCountIdentifier());
            context.setVariable("dataAccessCount",  accessCount.isPresent() ? accessCount.get().getCaption() : "0");
            var registeredCategories = riskAsset .getDataProcessing().getRegisteredCategories();
            context.setVariable("dataCategories", registeredCategories.stream().map(cat ->
                {
                    Optional<ChoiceValue> title = choiceService.getValue(cat.getPersonCategoriesRegisteredIdentifier());
                    List<String> types = cat.getPersonCategoriesInformationIdentifiers().stream().map(type -> Objects.requireNonNull(choiceService.getValue(type).orElse(null)).getCaption())
                        .filter(Objects::nonNull)
                        .toList();
                    if (title.isEmpty()) {return null;}
                    return new registeredDataCategory(title.get().getCaption(), types);
                })
                .filter(Objects::nonNull)
                .toList());
        }

        if (riskRegister != null) {
			context.setVariable("systemType", "Fortegnelse");
            String systemOwners = riskRegister.getResponsibleUsers().stream().map(User::getName).collect(Collectors.joining(", "));
            context.setVariable("systemOwners", systemOwners.isBlank() ? "Ikke udfyldt" : systemOwners);
            context.setVariable("deletionProcedureCreated", riskRegister.getDataProcessing().getDeletionProcedure() != null ? riskRegister.getDataProcessing().getDeletionProcedure().getMessage() : "Ikke udfyldt");
            context.setVariable("deletionProcedureLink", riskRegister.getDataProcessing().getDeletionProcedureLink());
            String dataAccessPersons = riskRegister.getDataProcessing().getAccessWhoIdentifiers().stream()
                .map(identifier ->
                {
                    Optional<ChoiceValue> value = choiceService.getValue(identifier);
                    if (value.isPresent()) {
                        return value.get().getCaption();
                    }
                    return "Ikke udfyldt";
                }).collect(Collectors.joining(", "));
            context.setVariable("dataAccessPersons", dataAccessPersons.isBlank() ? "Ikke udfyldt" : dataAccessPersons );
            var accessCount = choiceService.getValue(riskRegister.getDataProcessing().getAccessCountIdentifier());
            context.setVariable("dataAccessCount",  accessCount.isPresent() ? accessCount.get().getCaption() : "");
            var registeredCategories = riskRegister.getDataProcessing().getRegisteredCategories();
            context.setVariable("dataCategories", registeredCategories.stream().map(cat ->
                {
                    Optional<ChoiceValue> title = choiceService.getValue(cat.getPersonCategoriesRegisteredIdentifier());
                    List<String> types = cat.getPersonCategoriesInformationIdentifiers().stream().map(type -> Objects.requireNonNull(choiceService.getValue(type).orElse(null)).getCaption())
                        .filter(Objects::nonNull)
                        .toList();
                    return title.map(choiceValue -> new registeredDataCategory(choiceValue.getCaption(), types)).orElse(null);
                })
                .filter(Objects::isNull)
                .toList());
        }
        return context;
    }


    private Map<String,String> buildRiskProfileValueMap(List<RiskProfileDTO> riskProfiles) {
        Map<String,String> result = new HashMap<>();
        for (RiskProfileDTO riskProfile : riskProfiles) {
            String key = riskProfile.getConsequence() + "," + riskProfile.getProbability();
            if (result.containsKey(key)) {
                result.compute(key, (k, current) -> current + ", " + (riskProfile.getIndex() + 1));
            } else {
                result.put(key, Integer.toString(riskProfile.getIndex() + 1));
            }
        }
        return result;
    }

    private Map<String,String> buildRiskProfileValueMapAfter(List<RiskProfileDTO> riskProfiles) {
        Map<String,String> result = new HashMap<>();
        for (RiskProfileDTO riskProfile : riskProfiles) {
            if (riskProfile.getResidualConsequence() != -1 && riskProfile.getResidualProbability() != -1) {
                String key = riskProfile.getResidualConsequence() + "," + riskProfile.getResidualProbability();
                if (result.containsKey(key)) {
                    result.compute(key, (k, current) -> current + ", " + (riskProfile.getIndex() + 1) + "*");
                } else {
                    result.put(key, (riskProfile.getIndex() + 1) + "*");
                }
            } else {
                String key = riskProfile.getConsequence() + "," + riskProfile.getProbability();
                if (result.containsKey(key)) {
                    result.compute(key, (k, current) -> current + ", " + (riskProfile.getIndex() + 1));
                } else {
                    result.put(key, Integer.toString(riskProfile.getIndex() + 1));
                }
            }
        }
        return result;
    }

    record TaskPDFDTO(String name, String description, String taskType, String nextDeadline, String responsible, String department) {}
    private List<TaskPDFDTO> buildTasks(List<Task> riskAssessmentTasks) {
        List<TaskPDFDTO> result = new ArrayList<>();
        riskAssessmentTasks.forEach(
            task -> {
                result.add(new TaskPDFDTO(
                    task.getName(),
                    task.getDescription(),
                    task.getTaskType().getMessage(),
                    DK_DATE_FORMATTER.format(task.getNextDeadline()),
                    nullSafe(() -> task.getResponsibleUser().getName()),
                    nullSafe(() -> task.getResponsibleOu().getName())
                ));
            }
        );
        return result;
    }

    record RiskCalculationDTO(int probability, int consequence, int score, String color) {}
    record ThreatPDFDTO(int index,
                        String threatType,
                        String threat,
                        RiskCalculationDTO initialRisk,
                        String problem,
                        String existingMeasures,
                        String method,
                        String elaboration,
                        List<PrecautionDTO> linkedPrecautions,
                        RiskCalculationDTO residualRisk
    ) {}
    private List<ThreatPDFDTO> buildThreatsForPDF(Map<String, List<ThreatDTO>> threatList, List<RiskProfileDTO> riskProfiles, Map<String, String> colorMap) {
        List<ThreatPDFDTO> result = new ArrayList<>();
        threatList.forEach((threatType, threats) -> {
            threats.forEach(t -> {
                final RiskProfileDTO profile = riskProfiles.stream()
                    .filter(rp -> rp.getIndex() == t.getIndex())
                    .findFirst().orElse(null);
                if (profile != null) {
                    final String color = colorMap.get(profile.getConsequence() + "," + profile.getProbability());
                    final int score = profile.getProbability() * profile.getConsequence();
                    final String residualColor = colorMap.get(profile.getResidualConsequence() + "," + profile.getResidualProbability());
                    final int residualScore = profile.getResidualProbability() * profile.getResidualConsequence();
                    result.add(new ThreatPDFDTO(
                    t.getIndex() + 1,
                        threatType,
                        t.getThreat(),
                        new RiskCalculationDTO(
                            profile.getProbability(),
                            profile.getConsequence(),
                            score,
                            color),
                        t.getProblem(),
                        t.getExistingMeasures(),
                        t.getMethod() != null ? t.getMethod().getMessage() : "",
                        t.getElaboration(),
                        buildPrecautions(t.getRelatedPrecautions()
                            .stream()
                            .map(Precaution.class::cast)
                            .toList() ),
                        new RiskCalculationDTO(
                            profile.getResidualProbability(),
                            profile.getResidualConsequence(),
                            residualScore,
                            residualColor)
                    ));
                }
            });
        });
        return result;
    }

    private String getCriticality(final Asset asset, final Register register) {
        StringBuilder stringBuilder = new StringBuilder();
        if (asset != null) {
            stringBuilder.append("<p>");
            stringBuilder.append("Systemet er: ");
            stringBuilder.append(asset.getCriticality() != null ? asset.getCriticality().getMessage() : "Ikke udfyldt");
            stringBuilder.append("</p>");
            stringBuilder.append("<p>");
            stringBuilder.append("Nødplan: ");
            stringBuilder.append(asset.getEmergencyPlanLink() != null ? asset.getEmergencyPlanLink() : "Ikke udfyldt");
            stringBuilder.append("</p>");
        } else if (register != null) {
            stringBuilder.append("<p>");
            stringBuilder.append("Behandlingsaktiviteten er: ");
            stringBuilder.append(register.getCriticality() != null ? register.getCriticality().getMessage() : "Ikke udfyldt");
            stringBuilder.append("</p>");
            stringBuilder.append("<p>");
            stringBuilder.append("Nødplan: ");
            stringBuilder.append(register.getEmergencyPlanLink() != null ? register.getEmergencyPlanLink() : "Ikke udfyldt");
            stringBuilder.append("</p>");
        }
        return stringBuilder.toString();
    }

    private String getPresent(final ThreatAssessment threatAssessment) {
        if (threatAssessment.getPresentAtMeeting() == null || threatAssessment.getPresentAtMeeting().isEmpty()) {
            return null;
        }
        return threatAssessment.getPresentAtMeeting().stream().map(User::getName).collect(Collectors.joining(", "));
    }

    private String getSubHeading(final ThreatAssessment threatAssessment, final Asset asset, final Register register) {
        if (asset != null && asset.getResponsibleUsers() != null && !asset.getResponsibleUsers().isEmpty()) {
            return "Systemejere: " + asset.getResponsibleUsers().stream().map(User::getName).collect(Collectors.joining(", "));
        } else if (register != null && register.getResponsibleUsers() != null && !register.getResponsibleUsers().isEmpty()) {
            return "Behandlingsansvarlige: " + register.getResponsibleUsers().stream().map(User::getName).collect(Collectors.joining(", "));
        }
        if (threatAssessment.getResponsibleUser() != null) {
            return "Risikoejer: " + threatAssessment.getResponsibleUser().getName();
        }
        return "Risikoejer ikke udfyldt";
    }

    private byte[] convertHtmlToPdf(String html) throws IOException {
        var outputStream = new ByteArrayOutputStream();
        var renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(outputStream);
        var result = outputStream.toByteArray();
        outputStream.close();
        return result;
    }

    private static void setTaskRevisionInterval(final ThreatAssessment assessment, final Task task) {
        switch(assessment.getRevisionInterval()) {
            case YEARLY -> task.setRepetition(TaskRepetition.YEARLY);
            case EVERY_SECOND_YEAR -> task.setRepetition(TaskRepetition.EVERY_SECOND_YEAR);
            case EVERY_THIRD_YEAR -> task.setRepetition(TaskRepetition.EVERY_THIRD_YEAR);
            case NONE -> task.setRepetition(TaskRepetition.NONE);
        }
    }

}

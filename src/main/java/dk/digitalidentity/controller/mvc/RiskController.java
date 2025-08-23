package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.dao.TaskDao;
import dk.digitalidentity.event.EmailEvent;
import dk.digitalidentity.event.ThreatAssessmentUpdatedEvent;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.ConsequenceAssessment;
import dk.digitalidentity.model.entity.CustomThreat;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.EmailTemplate;
import dk.digitalidentity.model.entity.Precaution;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.DocumentType;
import dk.digitalidentity.model.entity.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.model.entity.enums.EmailTemplateType;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentReportApprovalStatus;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import dk.digitalidentity.model.entity.enums.ThreatDatabaseType;
import dk.digitalidentity.model.entity.enums.ThreatMethod;
import dk.digitalidentity.security.RequireSuperuserOrAdministrator;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.CatalogService;
import dk.digitalidentity.service.EmailTemplateService;
import dk.digitalidentity.service.RegisterService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.ScaleService;
import dk.digitalidentity.service.TaskService;
import dk.digitalidentity.service.ThreatAssessmentService;
import dk.digitalidentity.service.UserService;
import dk.digitalidentity.service.model.RiskDTO;
import dk.digitalidentity.service.model.TaskDTO;
import dk.digitalidentity.service.model.ThreatDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("risks")
@RequireUser
@RequiredArgsConstructor
public class RiskController {
    private final ApplicationEventPublisher eventPublisher;
    private final CatalogService catalogService;
    private final TaskDao taskDao;
    private final RelationService relationService;
    private final Environment environment;
    private final AssetService assetService;
    private final ThreatAssessmentService threatAssessmentService;
    private final ScaleService scaleService;
    private final TaskService taskService;
    private final RegisterService registerService;
    private final RelationDao relationDao;
    private final AssetDao assetDao;
    private final UserService userService;
    private final EmailTemplateService emailTemplateService;

    @GetMapping
    public String riskList(final Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("risk", new ThreatAssessment());
        model.addAttribute("threatCatalogs", catalogService.findAllVisible());
        model.addAttribute("superuser", authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)));
        return "risks/index";
    }

    @Transactional
    @RequireSuperuserOrAdministrator
    @PostMapping("create")
    public String formCreate(@Valid @ModelAttribute final ThreatAssessment threatAssessment,
            @RequestParam(name = "sendEmail", required = false) final boolean sendEmail,
            @RequestParam(name = "selectedRegister", required = false) final Long selectedRegister,
            @RequestParam(name = "presentAtMeeting", required = false) final Set<String> presentUserUuids,
            @RequestParam(name = "selectedAsset", required = false) final Set<Long> selectedAsset) {
        if (!threatAssessment.isRegistered() && !threatAssessment.isOrganisation() && !threatAssessment.isSociety()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der skal vælges minimum en af de tre vurderinger.");
        }
        if (threatAssessment.getThreatAssessmentType().equals(ThreatAssessmentType.ASSET) && (selectedAsset == null || selectedAsset.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der skal vælges et aktiv, når typen aktiv er valgt.");
        }
        if (threatAssessment.getThreatAssessmentType().equals(ThreatAssessmentType.REGISTER) && selectedRegister == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der skal vælges en behandlingsaktivitet, når typen behandlingsaktivitet er valgt.");
        }

        if (threatAssessment.getThreatAssessmentResponses() == null) {
            threatAssessment.setThreatAssessmentResponses(new ArrayList<>());
        }

        final ThreatAssessment savedThreatAssessment = threatAssessmentService.save(threatAssessment);
        savedThreatAssessment.setPresentAtMeeting(userService.findAllByUuids(presentUserUuids));
        if (threatAssessment.getThreatAssessmentType().equals(ThreatAssessmentType.ASSET)) {
            relateAssets(selectedAsset, savedThreatAssessment);
        } else if (threatAssessment.getThreatAssessmentType().equals(ThreatAssessmentType.REGISTER)) {
            relateRegister(selectedRegister, savedThreatAssessment);
        }
        if (sendEmail) {
            createTaskAndSendMail(savedThreatAssessment);
        }
        threatAssessmentService.setThreatAssessmentColor(savedThreatAssessment);
        eventPublisher.publishEvent(ThreatAssessmentUpdatedEvent.builder().threatAssessmentId(savedThreatAssessment.getId()).build());

        return "redirect:/risks/" + savedThreatAssessment.getId();
    }

    @GetMapping("{id}/edit")
    public String riskEditDialog(final Model model, @PathVariable("id") final long id) {
        final ThreatAssessment threatAssessment = threatAssessmentService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (threatAssessment.getThreatAssessmentType() == ThreatAssessmentType.ASSET) {
			final List<Relation> assetRelations = relationService.findRelatedToWithType(threatAssessment, RelationType.ASSET);
			model.addAttribute("relatedAssets", assetService.findAllByRelations(assetRelations));
		}

		model.addAttribute("threatCatalogs", catalogService.findAllVisible());
        model.addAttribute("risk", threatAssessment);
        return "risks/editForm";
    }

    @Transactional
    @PostMapping("{id}/edit")
    public String performEdit(@PathVariable("id") final long id,
                              @Valid @ModelAttribute final ThreatAssessment assessment,
                              @RequestParam(name = "presentAtMeeting", required = false) final Set<String> presentUserUuids,
								@RequestParam(name = "selectedAssets", required = false) final Set<Long> selectedAssets
	) {
        final ThreatAssessment editedAssessment = threatAssessmentService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !editedAssessment.getResponsibleUser().getUuid().equals(SecurityUtil.getPrincipalUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
		if (editedAssessment.getThreatAssessmentType().equals(ThreatAssessmentType.ASSET) && (selectedAssets == null || selectedAssets.isEmpty())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der skal vælges et aktiv, når typen aktiv er valgt.");
		}

		if (editedAssessment.getThreatAssessmentType().equals(ThreatAssessmentType.ASSET)) {
			relationService.setRelationsAbsolute(editedAssessment, selectedAssets);
		}
        editedAssessment.setName(assessment.getName());
        editedAssessment.setPresentAtMeeting(userService.findAllByUuids(presentUserUuids));
        editedAssessment.setResponsibleOu(assessment.getResponsibleOu());
        editedAssessment.setResponsibleUser(assessment.getResponsibleUser());

		// Handle threatCatalog changes
		threatAssessmentService.handleThreatCatalogChanges(editedAssessment, assessment.getThreatCatalogs());

        return "redirect:/risks";
    }

	@Transactional
	@PostMapping("{id}/update-catalogs")
	public String updateThreatCatalogs(@PathVariable("id") final long id, @RequestParam(name = "threatCatalogs", required = false) final Set<String> catalogIdentifiers) {
		final ThreatAssessment editedAssessment = threatAssessmentService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !editedAssessment.getResponsibleUser().getUuid().equals(SecurityUtil.getPrincipalUuid())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

		// Find selected catalogs
		List<ThreatCatalog> selectedCatalogs = new ArrayList<>();
		if (catalogIdentifiers != null && !catalogIdentifiers.isEmpty()) {
			selectedCatalogs = catalogService.findByIdentifierIn(catalogIdentifiers);
		}

		threatAssessmentService.handleThreatCatalogChanges(editedAssessment, selectedCatalogs);

		return "redirect:/risks/" + id;
	}

    @GetMapping("{id}/copy")
    public String riskCopyDialog(final Model model, @PathVariable("id") final long id) {
        final ThreatAssessment threatAssessment = threatAssessmentService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final List<Relation> registerRelations = relationService.findRelatedToWithType(threatAssessment, RelationType.REGISTER);
        final List<Relation> assetRelations = relationService.findRelatedToWithType(threatAssessment, RelationType.ASSET);
        model.addAttribute("risk", threatAssessment);
        model.addAttribute("relatedRegisters", registerService.findAllByRelations(registerRelations));
        model.addAttribute("relatedAssets", assetService.findAllByRelations(assetRelations));
        return "risks/copyForm";
    }

    @Transactional
    @RequireSuperuserOrAdministrator
    @PostMapping("{id}/copy")
    public String performCopy(@PathVariable("id") final long sourceId,
                              @Valid @ModelAttribute final ThreatAssessment assessment,
                              @RequestParam(name = "sendEmail", required = false) final boolean sendEmail,
                              @RequestParam(name = "selectedRegister", required = false) final Long selectedRegister,
                              @RequestParam(name = "presentAtMeeting", required = false) final Set<String> presentUserUuids,
                              @RequestParam(name = "selectedAsset", required = false) final Set<Long> selectedAsset) {
        if (assessment.getThreatAssessmentType().equals(ThreatAssessmentType.ASSET) && (selectedAsset == null || selectedAsset.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der skal vælges et aktiv.");
        }
        if (assessment.getThreatAssessmentType().equals(ThreatAssessmentType.REGISTER) && selectedRegister == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der skal vælges en behandlingsaktivitet");
        }
        final ThreatAssessment savedThreatAssessment = threatAssessmentService.copy(sourceId);
        savedThreatAssessment.setName(assessment.getName());
        savedThreatAssessment.setPresentAtMeeting(userService.findAllByUuids(presentUserUuids));
        if (assessment.getThreatAssessmentType().equals(ThreatAssessmentType.ASSET)) {
            relateAssets(selectedAsset, savedThreatAssessment);
        } else if (assessment.getThreatAssessmentType().equals(ThreatAssessmentType.REGISTER)) {
            relateRegister(selectedRegister, savedThreatAssessment);
        }
        if (sendEmail) {
            createTaskAndSendMail(savedThreatAssessment);
        }
        return "redirect:/risks/" + savedThreatAssessment.getId();
    }

	record SimpleThreatDTO(
			long id,
			long responseId,
			String identifier,
			ThreatDatabaseType dataType,
			String type,
			String threat,
			boolean notRelevant,
			int probability,
			int rf,
			int ri,
			int rt,
			int of,
			int oi,
			int ot,
			int sf,
			int si,
			int st,
			int sa,
			String problem,
			String existingMeasures,
			List<Precaution> relatedPrecautions,
			ThreatMethod method,
			String elaboration,
			int residualRiskConsequence,
			int residualRiskProbability,
			int index,
			List<TaskDTO> tasks
	) {
	}
    @GetMapping("{id}")
    public String risk(final Model model, @PathVariable final long id) {
        final ThreatAssessment threatAssessment = threatAssessmentService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("risk", threatAssessment);

		Map<String, List<SimpleThreatDTO>> threatsDto = new LinkedHashMap<>();
		for (var entryset : threatAssessmentService.buildThreatList(threatAssessment).entrySet()) {
			var simpleDtoList = entryset.getValue().stream()
					.map( threatDTO -> new SimpleThreatDTO(
							threatDTO.getId(),
							threatDTO.getResponseId(),
							threatDTO.getIdentifier(),
							threatDTO.getDataType(),
							threatDTO.getType(),
							threatDTO.getThreat(),
							threatDTO.isNotRelevant(),
							threatDTO.getProbability(),
							threatDTO.getRf(),
							threatDTO.getRi(),
							threatDTO.getRt(),
							threatDTO.getOf(),
							threatDTO.getOi(),
							threatDTO.getOt(),
							threatDTO.getSf(),
							threatDTO.getSi(),
							threatDTO.getSt(),
							threatDTO.getSa(),
							threatDTO.getProblem(),
							threatDTO.getExistingMeasures(),
							(threatDTO.getRelatedPrecautions().stream().map(r -> (Precaution) r).toList()),
							threatDTO.getMethod(),
							threatDTO.getElaboration(),
							threatDTO.getResidualRiskConsequence(),
							threatDTO.getResidualRiskProbability(),
							threatDTO.getIndex(),
							threatDTO.getTasks()
					)).toList();
			threatsDto.put(entryset.getKey(), simpleDtoList);
		}
        model.addAttribute("threats", threatsDto);
        model.addAttribute("elementName", findElementName(threatAssessment));
        model.addAttribute("customThreat", new CustomThreat());
        model.addAttribute("scale", new TreeMap<>(scaleService.getConsequenceScale()));
        model.addAttribute("riskScoreColorMap", scaleService.getScaleRiskScoreColorMap());
        model.addAttribute("probabilityExplainer", scaleService.getScaleProbabilityNumberExplainer());
        model.addAttribute("consequenceExplainer", scaleService.getScaleConsequenceNumberExplainer());
        model.addAttribute("riskScoreExplainer", scaleService.getScaleRiskScoreExplainer());
        model.addAttribute("tasks", taskService.buildRelatedTasks(threatAssessment, false));
        model.addAttribute("relatedRegisters", findRelatedRegisters(threatAssessment));
        model.addAttribute("presentAtMeetingName", threatAssessment.getPresentAtMeeting().stream().map(User::getName).collect(Collectors.joining(", ")));
        model.addAttribute("defaultSendReportTo", getFirstRelatedResponsible(threatAssessment));
        model.addAttribute("threatCatalogs", catalogService.findAllVisible());

        boolean signed = threatAssessment.getThreatAssessmentReportApprovalStatus().equals(ThreatAssessmentReportApprovalStatus.SIGNED) && threatAssessment.getThreatAssessmentReportS3Document() != null;
        model.addAttribute("signed", signed);

        final Document document = new Document();
        document.setDocumentType(DocumentType.PROCEDURE);
        model.addAttribute("document", document);
        return "risks/view";
    }

    record RelatedRegisterDTO(long registerId, String registerName, Integer rf, Integer ri, Integer rt, Integer of, Integer oi, Integer ot, Integer sf, Integer si, Integer st) {}
    private List<RelatedRegisterDTO> findRelatedRegisters(final ThreatAssessment threatAssessment) {
        final List<RelatedRegisterDTO> result = new ArrayList<>();
        if (threatAssessment.isInherit() && ThreatAssessmentType.ASSET.equals(threatAssessment.getThreatAssessmentType())) {
            final List<Relation> relations = relationDao.findRelatedToWithType(threatAssessment.getId(), RelationType.ASSET);
            final List<Asset> assets = relations.stream()
                .map(r -> r.getRelationAType().equals(RelationType.ASSET) ? r.getRelationAId() : r.getRelationBId())
                .map(assetDao::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

            final List<Long> addedIds = new ArrayList<>();
            for (final Asset asset : assets) {
                final List<Relatable> allRelatedTo = relationService.findAllRelatedTo(asset);
                final List<Register> registers = allRelatedTo.stream().filter(r -> r.getRelationType() == RelationType.REGISTER).map(r -> (Register) r).toList();
                for (final Register register : registers) {
                    if (!addedIds.contains(register.getId())) {
                        final ConsequenceAssessment consequenceAssessment = register.getConsequenceAssessment();
                        if (consequenceAssessment != null) {
                            result.add(new RelatedRegisterDTO(register.getId(), register.getName(), consequenceAssessment.getConfidentialityRegistered(), consequenceAssessment.getIntegrityRegistered(), consequenceAssessment.getAvailabilityRegistered(), consequenceAssessment.getConfidentialityOrganisation(), consequenceAssessment.getIntegrityOrganisation(), consequenceAssessment.getAvailabilityOrganisation(), consequenceAssessment.getConfidentialitySociety(), consequenceAssessment.getIntegritySociety(), consequenceAssessment.getAvailabilitySociety()));
                        } else {
                            result.add(new RelatedRegisterDTO(register.getId(), register.getName(), null, null, null, null, null, null, null, null, null));
                        }

                        addedIds.add(register.getId());
                    }
                }
            }
        }

        return result;
    }

    @GetMapping("{id}/profile")
    public String riskProfile(final Model model, @PathVariable final long id) {
        final ThreatAssessment threatAssessment = threatAssessmentService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("risk", threatAssessment);
        model.addAttribute("elementName", findElementName(threatAssessment));
        model.addAttribute("reversedScale", scaleService.getConsequenceScale().keySet().stream().sorted(Collections.reverseOrder()).collect(Collectors.toList()));
        model.addAttribute("riskScoreColorMap", scaleService.getScaleRiskScoreColorMap());
        model.addAttribute("riskProfiles", threatAssessmentService.buildRiskProfileDTOs(threatAssessment));
        model.addAttribute("riskScoreColorMap", scaleService.getScaleRiskScoreColorMap());

        return "risks/profile";
    }

    @GetMapping("{id}/revision")
    public String revisionForm(final Model model, @PathVariable final long id) {
        final ThreatAssessment threatAssessment = threatAssessmentService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        threatAssessmentService.updateNextRevisionAssociatedTask(threatAssessment);
        model.addAttribute("risk", threatAssessment);
        return "risks/revisionIntervalForm";
    }

    @PostMapping("{id}/revision")
    @Transactional
    public String postRevisionForm(@ModelAttribute final ThreatAssessment assessment, @PathVariable final long id) {
        final ThreatAssessment threatAssessment = threatAssessmentService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !threatAssessment.getResponsibleUser().getUuid().equals(SecurityUtil.getPrincipalUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        threatAssessment.setRevisionInterval(assessment.getRevisionInterval());
        threatAssessment.setNextRevision(assessment.getNextRevision());
        threatAssessmentService.createOrUpdateAssociatedCheck(threatAssessment);
        return "redirect:/risks/" + assessment.getId();
    }

    @DeleteMapping("{id}")
    @RequireSuperuserOrAdministrator
    @ResponseStatus(value = HttpStatus.OK)
    @Transactional
    public void riskDelete(@PathVariable final Long id) {
        final ThreatAssessment threatAssessment = threatAssessmentService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // All related checks should be deleted along with the threatAssessment
        final List<Task> tasks = taskService.findRelatedTasks(threatAssessment, t -> t.getTaskType() == TaskType.CHECK);
        taskDao.deleteAll(tasks);

        threatAssessmentService.deleteById(id);
    }

	public record CustomThreatDTO (Long id, String threatType, String description) {}
    @Transactional
    @RequireSuperuserOrAdministrator
    @PostMapping("{id}/customthreats/create")
    public String formCreateCustomThreat(@PathVariable final long id, @Valid @ModelAttribute final CustomThreatDTO customThreatDTO) {
        final ThreatAssessment threatAssessment = threatAssessmentService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		CustomThreat customThreat = new CustomThreat();
		customThreat.setThreatType(customThreatDTO.threatType);
		customThreat.setDescription(customThreatDTO.description);
        customThreat.setThreatAssessment(threatAssessment);
		threatAssessment.getCustomThreats().add(customThreat);
        threatAssessmentService.save(threatAssessment);
        eventPublisher.publishEvent(ThreatAssessmentUpdatedEvent.builder().threatAssessmentId(id).build());

        return "redirect:/risks/" + id;
    }

	private String findElementName(final ThreatAssessment threatAssessment) {
        final ThreatAssessmentType threatAssessmentType = threatAssessment.getThreatAssessmentType();
        if (ThreatAssessmentType.ASSET.equals(threatAssessmentType)) {
            final List<Relation> relations = relationService.findRelatedToWithType(threatAssessment, RelationType.ASSET);
            return relations.stream()
                .map(r -> r.getRelationAType().equals(RelationType.ASSET) ? r.getRelationAId() : r.getRelationBId())
                .map(assetService::findById)
                .filter(Optional::isPresent)
                .map(a -> a.get().getName())
                .collect(Collectors.joining(", "));
        } else if (ThreatAssessmentType.REGISTER.equals(threatAssessmentType)) {
            final List<Relation> relations = relationService.findRelatedToWithType(threatAssessment, RelationType.REGISTER);
            return relations.stream()
                .map(r -> r.getRelationAType().equals(RelationType.REGISTER) ? r.getRelationAId() : r.getRelationBId())
                .map(registerService::findById)
                .filter(Optional::isPresent)
                .map(a -> a.get().getName())
                .collect(Collectors.joining(", "));
        } else {
            return "";
        }
    }

    private void createTaskAndSendMail(final ThreatAssessment savedThreatAssessment) {
        if (savedThreatAssessment.getResponsibleUser() != null) {
            EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.RISK_REMINDER);
            if (template.isEnabled()) {
                final Task task = threatAssessmentService.createAssociatedTask(savedThreatAssessment);
                if (task != null && !StringUtils.isEmpty(task.getResponsibleUser().getEmail())) {
                    final String url = environment.getProperty("di.saml.sp.baseUrl") + "/tasks/" +  task.getId();
                    final String recipient = task.getResponsibleUser().getName();
                    final String objectName = task.getName();
                    final String link = "<a href=\"" + url + "\">" + url + "</a>";

                    String title = template.getTitle();
                    title = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient);
                    title = title.replace(EmailTemplatePlaceholder.OBJECT_PLACEHOLDER.getPlaceholder(), objectName);
                    title = title.replace(EmailTemplatePlaceholder.LINK_PLACEHOLDER.getPlaceholder(), link);
                    String message = template.getMessage();
                    message = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient);
                    message = message.replace(EmailTemplatePlaceholder.OBJECT_PLACEHOLDER.getPlaceholder(), objectName);
                    message = message.replace(EmailTemplatePlaceholder.LINK_PLACEHOLDER.getPlaceholder(), link);
                    eventPublisher.publishEvent(EmailEvent.builder()
                        .message(message)
                        .subject(title)
                        .email(task.getResponsibleUser().getEmail())
						.templateType(template.getTemplateType())
                        .build());
                }
            } else {
                log.info("Email template with type " + template.getTemplateType() + " is disabled. Email was not sent.");
            }
        }
    }

    private void relateAssets(final Set<Long> selectedAsset, final ThreatAssessment savedThreatAssessment) {
        final List<Asset> relatedAssets = assetService.findAllById(selectedAsset);
        relatedAssets.forEach(asset -> relationService.addRelation(savedThreatAssessment, asset));
        if (savedThreatAssessment.isInherit()) {
            threatAssessmentService.inheritRisk(savedThreatAssessment, relatedAssets);
        }
    }

    private void relateRegister(final Long selectedRegister, final ThreatAssessment savedThreatAssessment) {
        final Register register = registerService.findById(selectedRegister).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der skal vælges en behandlingsaktivitet, når typen behandlingsaktivitet er valgt."));
        relationService.addRelation(savedThreatAssessment, register);
    }

    private User getFirstRelatedResponsible(final ThreatAssessment threatAssessment) {
        if (threatAssessment.getThreatAssessmentType() == ThreatAssessmentType.ASSET) {
            final List<Asset> assets =  relationService.findRelatedToWithType(threatAssessment, RelationType.ASSET).stream()
                .map(a -> a.getRelationAType() == RelationType.ASSET ? a.getRelationAId() : a.getRelationBId())
                .map(assetService::findById)
                .filter(Optional::isPresent)
                .map(Optional::get).toList();
            for (final Asset asset : assets) {
                if (asset.getResponsibleUsers() != null) {
                    for (final User responsibleUser : asset.getResponsibleUsers()) {
                        if (responsibleUser.getEmail() != null) {
                            return responsibleUser;
                        }
                    }
                }
            }

        } else if (threatAssessment.getThreatAssessmentType() == ThreatAssessmentType.REGISTER) {
            final List<Register> registers = relationService.findRelatedToWithType(threatAssessment, RelationType.REGISTER).stream()
                .map(a -> a.getRelationAType() == RelationType.REGISTER ? a.getRelationAId() : a.getRelationBId())
                .map(registerService::findById)
                .filter(Optional::isPresent)
                .map(Optional::get).toList();

            for (final Register register : registers) {
                if (register.getResponsibleUsers() != null) {
                    for (final User responsibleUser : register.getResponsibleUsers()) {
                        if (responsibleUser.getEmail() != null) {
                            return responsibleUser;
                        }
                    }
                }
            }
        }
        return null;
    }

	record ResponsibleUserDTO(String uuid, String name) {}
	record ResponsibleOUDTO (String uuid, String name ) {}
	record SimpleEditRiskAsset(Long id, String name) {}
	record ExternalThreatAssessmentEditDTO(Long id, String name, ThreatAssessmentType threatAssessmentType, ResponsibleOUDTO responsibleOu, ResponsibleUserDTO responsibleUser, List<SimpleEditRiskAsset> relatedAssets, String externalLink) {}
    @GetMapping("external/{riskId}/edit")
    public String riskId(final Model model, @PathVariable Long riskId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("superuser", authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)));

		ThreatAssessment riskassessment = threatAssessmentService.findById(riskId)
				.orElseThrow();
		final List<Relation> assetRelations = relationService.findRelatedToWithType(riskassessment, RelationType.ASSET);
		var externalDTO = new ExternalThreatAssessmentEditDTO(
				riskassessment.getId(),
				riskassessment.getName(),
				riskassessment.getThreatAssessmentType(),
				riskassessment.getResponsibleOu() != null ? new ResponsibleOUDTO(riskassessment.getResponsibleOu().getUuid(), riskassessment.getResponsibleOu().getName()) : null,
				riskassessment.getResponsibleUser() != null ? new ResponsibleUserDTO(riskassessment.getResponsibleUser().getUuid(), riskassessment.getResponsibleUser().getName()) : null,
				assetService.findAllByRelations(assetRelations).stream().map(a -> new SimpleEditRiskAsset(a.getId(), a.getName())).toList(),
				riskassessment.getExternalLink()
		);

        model.addAttribute("risk", externalDTO);
        return "risks/fragments/edit_external_riskassessment_modal :: create_external_riskassessment_modal";
    }

	record ExternalThreatAssessmentCreateDTO(Long id, String name, ThreatAssessmentType threatAssessmentType, ResponsibleOUDTO responsibleOu, ResponsibleUserDTO responsibleUser, String externalLink) {}
    @GetMapping("external/create")
    public String createExternalDPIA(final Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("superuser", authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)));
        model.addAttribute("risk", new ExternalThreatAssessmentCreateDTO(null, "", ThreatAssessmentType.ASSET, new ResponsibleOUDTO("", ""), new ResponsibleUserDTO("", ""), "")); // emppty dto
        return "risks/fragments/create_external_riskassessment_modal :: create_external_riskassessment_modal";
    }

	@GetMapping("dashboard")
	public String riskDashboard(final Model model) {
		model.addAttribute("reversedScale", scaleService.getConsequenceScale().keySet().stream()
				.sorted(Collections.reverseOrder())
				.collect(Collectors.toList()));

		model.addAttribute("riskScoreColorMap", scaleService.getScaleRiskScoreColorMap());

		// Add available years for development over time chart
		List<Integer> availableYears = getAvailableYearsForRiskAssessments();
		model.addAttribute("availableYears", availableYears);

		return "risks/dashboard";
	}

	private List<Integer> getAvailableYearsForRiskAssessments() {
		// Get all non-deleted threat assessments
		List<ThreatAssessment> allAssessments = threatAssessmentService.findAllNotDeleted();

		// Extract years from createdAt dates and return sorted unique years
		return allAssessments.stream()
				.filter(assessment -> assessment.getCreatedAt() != null)
				.map(assessment -> assessment.getCreatedAt().getYear())
				.distinct()
				.sorted()
				.collect(Collectors.toList());
	}
}

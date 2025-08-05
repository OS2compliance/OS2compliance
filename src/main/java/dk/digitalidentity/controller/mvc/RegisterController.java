package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.ConsequenceAssessmentDao;
import dk.digitalidentity.model.dto.DataProcessingDTO;
import dk.digitalidentity.model.dto.RegisterAssetRiskDTO;
import dk.digitalidentity.model.dto.RelationDTO;
import dk.digitalidentity.model.dto.SelectionChoiceDTO;
import dk.digitalidentity.model.dto.SelectionDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetSupplierMapping;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.ConsequenceAssessment;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.RelationProperty;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.Criticality;
import dk.digitalidentity.model.entity.enums.InformationObligationStatus;
import dk.digitalidentity.model.entity.enums.RegisterSetting;
import dk.digitalidentity.model.entity.enums.RegisterStatus;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.kle.KLEGroup;
import dk.digitalidentity.model.entity.kle.KLELegalReference;
import dk.digitalidentity.model.entity.kle.KLEMainGroup;
import dk.digitalidentity.model.entity.kle.KLESubject;
import dk.digitalidentity.security.RequireSuperuserOrAdministrator;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.DataProcessingService;
import dk.digitalidentity.service.OrganisationService;
import dk.digitalidentity.service.RegisterAssetAssessmentService;
import dk.digitalidentity.service.RegisterService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.ScaleService;
import dk.digitalidentity.service.SettingsService;
import dk.digitalidentity.service.TaskService;
import dk.digitalidentity.service.UserService;
import dk.digitalidentity.service.kle.KLEGroupService;
import dk.digitalidentity.service.kle.KLELegalReferenceService;
import dk.digitalidentity.service.kle.KLEMainGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
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
import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static dk.digitalidentity.util.ComplianceStringUtils.asNumber;

@Slf4j
@Controller
@RequireUser
@RequestMapping("registers")
@RequiredArgsConstructor
public class RegisterController {
    private final RegisterService registerService;
    private final RegisterAssetAssessmentService registerAssetAssessmentService;
    private final AssetService assetService;
    private final RelationService relationService;
    private final ChoiceService choiceService;
    private final OrganisationService organisationService;
    private final ConsequenceAssessmentDao consequenceAssessmentDao;
    private final ScaleService scaleService;
    private final DataProcessingService dataProcessingService;
    private final TaskService taskService;
    private final UserService userService;
	private final SettingsService settingsService;
	private final KLEMainGroupService kLEMainGroupService;
	private final KLEGroupService kLEGroupService;
	private final KLELegalReferenceService kLELegalReferenceService;

	@GetMapping
	public String registerList(Model model) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		model.addAttribute("superuser", authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)));
		return "registers/index";
	}


    @GetMapping("form")
    public String form(final Model model, @RequestParam(name = "id", required = false) final Long id) {
        if (id == null) {
            model.addAttribute("register", new Register());
            model.addAttribute("formId", "createForm");
            model.addAttribute("formTitle", "Ny behandlingsaktivitet");
            model.addAttribute("action", "/registers/create");
        } else {
            final Register register = registerService.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            model.addAttribute("register", register);
            model.addAttribute("formId", "editForm");
            model.addAttribute("formTitle", "Rediger behandlingsaktivitet");
            model.addAttribute("action", "/registers/" + register.getId() + "/update?showIndex=true");
        }
        return "registers/form";
    }

    @RequireSuperuserOrAdministrator
    @Transactional
    @PostMapping("create")
    public String create(@ModelAttribute @Valid final Register register) {
        final Register saved = registerService.save(register);
        return "redirect:/registers/" + saved.getId();
    }

    @Transactional
    @PostMapping("{id}/assessment")
    public String updateAssessment(@PathVariable final Long id,
                                   @ModelAttribute @Valid final ConsequenceAssessment assessment,
                                   @RequestParam(required = false) final String section) {
        final Optional<ConsequenceAssessment> existingOptional = consequenceAssessmentDao.findById(id);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (existingOptional.isPresent()) {
            final ConsequenceAssessment existing = existingOptional.get();
            ensureEditingIsAllowed(existing.getRegister());

            existing.setAssessment(assessment.getAssessment());
            existing.setConfidentialityRegistered(assessment.getConfidentialityRegistered());
            existing.setConfidentialityOrganisation(assessment.getConfidentialityOrganisation());
            existing.setConfidentialityOrganisationRep(assessment.getConfidentialityOrganisationRep());
            existing.setConfidentialityOrganisationEco(assessment.getConfidentialityOrganisationEco());
            existing.setConfidentialitySociety(assessment.getConfidentialitySociety());
            existing.setConfidentialityReason(assessment.getConfidentialityReason());

            existing.setIntegrityRegistered(assessment.getIntegrityRegistered());
            existing.setIntegrityOrganisation(assessment.getIntegrityOrganisation());
            existing.setIntegrityOrganisationRep(assessment.getIntegrityOrganisationRep());
            existing.setIntegrityOrganisationEco(assessment.getIntegrityOrganisationEco());
            existing.setIntegritySociety(assessment.getIntegritySociety());
            existing.setIntegrityReason(assessment.getIntegrityReason());

            existing.setAvailabilityRegistered(assessment.getAvailabilityRegistered());
            existing.setAvailabilityOrganisation(assessment.getAvailabilityOrganisation());
            existing.setAvailabilityOrganisationRep(assessment.getAvailabilityOrganisationRep());
            existing.setAvailabilityOrganisationEco(assessment.getAvailabilityOrganisationEco());
            existing.setAvailabilitySociety(assessment.getAvailabilitySociety());
            existing.setAvailabilityReason(assessment.getAvailabilityReason());
        } else {
            assessment.setId(null);
            final Register register = registerService.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER) && register.getResponsibleUsers().stream().map(User::getUuid).toList().contains(SecurityUtil.getPrincipalUuid()))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            assessment.setRegister(register);
            register.setConsequenceAssessment(assessment);
            consequenceAssessmentDao.save(assessment);
        }

        return "redirect:/registers/" + id + (section != null ? "?section=" + section : "");
    }

    @PostMapping("{id}/update")
	public String update(@PathVariable final Long id,
			@RequestParam(value = "showIndex", required = false, defaultValue = "false") final boolean showIndex,
			@RequestParam(value = "name", required = false) @Valid final String name,
			@RequestParam(value = "description", required = false) @Valid final String description,
			@RequestParam(value = "responsibleOus", required = false) @Valid final Set<String> responsibleOuUuids,
			@RequestParam(value = "departments", required = false) @Valid final Set<String> departmentUuids,
			@RequestParam(value = "responsibleUsers", required = false) @Valid final Set<String> responsibleUserUuids,
			@RequestParam(value = "customResponsibleUsers", required = false) @Valid final Set<String> customResponsibleUserUuids,
			@RequestParam(value = "criticality", required = false) final Criticality criticality,
			@RequestParam(value = "emergencyPlanLink", required = false) final String emergencyPlanLink,
			@RequestParam(value = "informationResponsible", required = false) final String informationResponsible,
			@RequestParam(value = "dataProtectionOfficer", required = false) final String dataProtectionOfficer,
			@RequestParam(value = "registerRegarding", required = false) final Set<ChoiceValue> registerRegarding,
			@RequestParam(value = "securityPrecautions", required = false) final String securityPrecautions,
			@RequestParam(required = false) final String section,
			@RequestParam(value = "status", required = false) final RegisterStatus status,
			@RequestParam(value = "mainGroups", required = false) final Set<String> mainGroupIds,
			@RequestParam(value = "groups", required = false) final Set<String> groupIds
			) {
        final Register register = registerService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ensureEditingIsAllowed(register);
        if (name != null) {
            register.setName(name);
        }
        if (description != null) {
            register.setDescription(description);
        }
        if (responsibleOuUuids != null && !responsibleOuUuids.isEmpty()) {
            final List<OrganisationUnit> responsibleOus = organisationService.findAllByUuids(responsibleOuUuids);
            register.setResponsibleOus(responsibleOus);
        } else {
            register.setResponsibleOus(null);
        }
        if (departmentUuids != null && !departmentUuids.isEmpty()) {
            final List<OrganisationUnit> departmentOus = organisationService.findAllByUuids(departmentUuids);
            register.setDepartments(departmentOus);
        } else {
            register.setDepartments(null);
        }
        if (responsibleUserUuids != null && !responsibleUserUuids.isEmpty()) {
            final List<User> responsibleUsers = userService.findAllByUuids(responsibleUserUuids);
            register.setResponsibleUsers(responsibleUsers);
        } else {
            register.setResponsibleUsers(null);
        }
		if (customResponsibleUserUuids != null && !customResponsibleUserUuids.isEmpty()) {
			final List<User> customResponsibleUsers = userService.findAllByUuids(customResponsibleUserUuids);
			register.setCustomResponsibleUsers(customResponsibleUsers);
		} else {
			register.setCustomResponsibleUsers(new ArrayList<>());
		}
        if (emergencyPlanLink != null) {
            register.setEmergencyPlanLink(emergencyPlanLink);
        }
        if (informationResponsible != null) {
            register.setInformationResponsible(informationResponsible);
        }
		if (dataProtectionOfficer != null) {
			register.setDataProtectionOfficer(dataProtectionOfficer);
		}

		register.setRegisterRegarding(registerRegarding);

		if (securityPrecautions != null) {
			register.setSecurityPrecautions(securityPrecautions);
		}
        if (criticality != null) {
            register.setCriticality(criticality);
        }
        if (status != null) {
            register.setStatus(status);
        }

		if (mainGroupIds != null && !mainGroupIds.isEmpty()) {
			register.setKleMainGroups(kLEMainGroupService.getAllByMainGroupNumbers(mainGroupIds));
		} else {
			register.setKleMainGroups(new HashSet<>());
		}
		if (groupIds != null && !groupIds.isEmpty()) {
			register.setKleGroups(kLEGroupService.getAllByGroupNumbers(groupIds));
		} else {
			register.setKleGroups(new HashSet<>());
		}

        registerService.save(register);
        return showIndex ? "redirect:/registers" : "redirect:/registers/" + id + (section != null ? "?section=" + section : "");
    }

    @Transactional
    @PostMapping("{id}/purpose")
    public String purpose(
			@PathVariable final Long id,
			@RequestParam(value = "purpose", required = false) final String purpose,
			@RequestParam(value = "gdprChoices", required = false) final Set<String> gdprChoices,
			@RequestParam(value = "informationObligation", required = false) final InformationObligationStatus informationObligationStatus,
			@RequestParam(value = "informationObligationDesc", required = false) final String informationObligationDesc,
			@RequestParam(value = "consent", required = false) final String consent,
			@RequestParam(value = "purposeNotes", required = false) final String purposeNotes,
			@RequestParam(value = "relevantLegalReferences", required = false) final Set<String> relevantLegalReferences,
			@RequestParam(value = "supplementalLegalBasis", required = false) final String supplementalLegalBasis
	) {
        final Register register = registerService.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ensureEditingIsAllowed(register);
        if (purpose != null) {
            register.setPurpose(purpose);
        }
        if (gdprChoices != null) {
            register.setGdprChoices(gdprChoices);
        }
        if (purposeNotes != null) {
            register.setPurposeNotes(purposeNotes);
        }
        if (consent != null) {
            register.setConsent(consent);
        }
		if (supplementalLegalBasis != null) {
			register.setSupplementalLegalBasis(supplementalLegalBasis);
		}
        if (informationObligationStatus != null) {
            register.setInformationObligation(informationObligationStatus);
        }
        if (informationObligationDesc != null) {
            register.setInformationObligationDesc(informationObligationDesc);
        }
		if(relevantLegalReferences != null && !relevantLegalReferences.isEmpty()) {
			register.setRelevantKLELegalReferences(kLELegalReferenceService.getAllWithAccessionNumberIn(relevantLegalReferences));
		}

		registerService.save(register);
        return "redirect:/registers/" + id + "?section=purpose";
    }

    @Transactional
    @PostMapping("{id}/dataprocessing")
    public String dataProcessing(@PathVariable final Long id, @Valid @ModelAttribute final DataProcessingDTO body) {
        final Register register = registerService.findById(body.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        ensureEditingIsAllowed(register);
        dataProcessingService.update(register.getDataProcessing(), body);
        return "redirect:/registers/" + id + "?section=dataprocessing";
    }

    @GetMapping("{id}")
    public String view(final Model model, @PathVariable final Long id,
                       @RequestParam(required = false) final String section) {
        final Register register = registerService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final ConsequenceAssessment assessment = consequenceAssessmentDao.findById(id)
                .orElse(new ConsequenceAssessment());
        final List<Relatable> allRelatedTo = relationService.findAllRelatedTo(register);
        final ChoiceList gdprP6ChoiceList = choiceService.findChoiceList("register-gdpr-p6")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not find GDPR Choices"));
        final List<ChoiceValue> gdprP6Choices = sortChoicesAlpha(gdprP6ChoiceList);
        final ChoiceList gdprP7ChoiceList = choiceService.findChoiceList("register-gdpr-p7")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not find GDPR Choices"));
        final List<ChoiceValue> gdprP7Choices = sortChoicesAlpha(gdprP7ChoiceList);
        final ChoiceList gdprChoiceList = choiceService.findChoiceList("register-gdpr")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not find GDPR Choices"));
        final List<ChoiceValue> gdprChoices = sortChoicesNumeric(gdprChoiceList);

        final List<RelationDTO<Register, Relatable>> relatedAssets = relationService.findRelations(register, RelationType.ASSET);
        final List<Pair<Integer, AssetSupplierMapping>> assetSupplierMappingList = registerAssetAssessmentService.assetSupplierMappingList(relatedAssets);
        final List<RegisterAssetRiskDTO> assetThreatAssessments = registerAssetAssessmentService.assetThreatAssessments(assetSupplierMappingList);

		final List<SelectionDTO> mainGroups = kLEMainGroupService.getAll().stream()
				.sorted(Comparator.comparing(KLEMainGroup::getMainGroupNumber))
				.map(mg -> new SelectionDTO(mg.getMainGroupNumber()+" "+mg.getTitle(), mg.getMainGroupNumber(), register.getKleMainGroups().contains(mg)))
				.toList();
		model.addAttribute("mainGroups", mainGroups);

		final Set<KLEGroup> kleGroups = kLEGroupService.getAllForMainGroups(register.getKleMainGroups());
		model.addAttribute("kleGroups", kleGroups.stream()
				.sorted(Comparator.comparing(KLEGroup::getGroupNumber))
				.map(g -> new SelectionDTO(g.getGroupNumber() +" " + g.getTitle(), g.getGroupNumber(), register.getKleGroups().contains(g))));

		final Set<String> selectedLegalReferenceAccessionNumbers = register.getRelevantKLELegalReferences().stream().map(KLELegalReference::getAccessionNumber).collect(Collectors.toSet());
		final Set<SelectionDTO> kleLegalReferences = register.getKleGroups().stream()
				.flatMap(g -> g.getLegalReferences().stream())
				.map(lr -> new SelectionDTO(lr.getTitle(), lr.getAccessionNumber(), selectedLegalReferenceAccessionNumbers.contains(lr.getAccessionNumber())))
				.collect(Collectors.toSet());
		model.addAttribute("kleLegalReferences", kleLegalReferences);

		model.addAttribute("selectedKleMainGroups", toSelectedMainGroupDTOs(register.getKleMainGroups(), register.getKleGroups()));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		model.addAttribute("customResponsibleUserFieldName", settingsService.getString(RegisterSetting.CUSTOMRESPONSIBLEUSERFIELDNAME.getValue(), "Ansvarlig for udfyldelse"));

		model.addAttribute("recordOfProcessingActivityRegardingChoices", choiceService.findChoiceValuesForListIdentifier("record-of-processing-activity-regarding").stream()
				.map(cv -> new SelectionChoiceDTO(cv.getCaption(), cv.getId().toString(), register.getRegisterRegarding().contains(cv))));

        model.addAttribute("section", section);
		model.addAttribute("changeableRegister", (authentication.getAuthorities().stream()
				.anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER) || r.getAuthority().equals(Roles.ADMINISTRATOR))
				|| register.getResponsibleUsers().stream().anyMatch(user -> user.getUuid().equals(SecurityUtil.getPrincipalUuid())))
				|| register.getCustomResponsibleUsers().stream().anyMatch(user -> user.getUuid().equals(SecurityUtil.getPrincipalUuid()))
		);
        model.addAttribute("dpChoices", dataProcessingService.getChoices());
        model.addAttribute("dataProcessing", register.getDataProcessing());
        model.addAttribute("register", register);
        model.addAttribute("assessment", assessment);
        model.addAttribute("gdprChoices", gdprChoices);
        model.addAttribute("gdprP6Choices", gdprP6Choices);
        model.addAttribute("gdprP7Choices", gdprP7Choices);
        model.addAttribute("relatedDocuments", allRelatedTo.stream()
                .filter(r -> r.getRelationType() == RelationType.DOCUMENT)
				.toList());

		record TaskListDTO(long id, String title, String responsibleUserName, String responsibleOuName, String taskType, String deadline, String repeats, String status, RelationType relationType){}
		model.addAttribute("relatedTasks", allRelatedTo.stream()
				.filter(r -> r.getRelationType() == RelationType.TASK)
				.map(r -> {
					Task task = ((Task) r);
					return new TaskListDTO(
							task.getId(),
							task.getName(),
							task.getResponsibleUser().getName(),
							task.getResponsibleOu().getName(),
							task.getTaskType().getMessage(),
							task.getNextDeadline().toString(),
							task.getRepetition().getMessage(),
							taskService.findHtmlStatusBadgeForTask(task),
							RelationType.TASK
					);
				})
				.toList());
        model.addAttribute("relatedAssets", relatedAssets);
        model.addAttribute("threatAssessments", allRelatedTo.stream()
            .filter(r -> r.getRelationType() == RelationType.THREAT_ASSESSMENT)
				.toList());
        model.addAttribute("assetThreatAssessments", assetThreatAssessments);
        model.addAttribute("scale", new TreeMap<>(scaleService.getConsequenceScale()));
        model.addAttribute("consequenceScale", scaleService.getConsequenceNumberDescriptions());
        model.addAttribute("relatedAssetsSubSuppliers", assetSupplierMappingList);
		model.addAttribute("risk", new ThreatAssessment());
		model.addAttribute("superuser", authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)));

        return "registers/view";
    }

	record SelectedLegalReferenceDTO(String accessionNumber, String title, String paragraph) {}
	record SelectedKLESubjectDTO(String subjectNumber, String title, String preservationCode, String durationBeforeDeletion, Set<SelectedLegalReferenceDTO> legalReferences){}
	record SelectedKLEGroupDTO (String groupNumber, String title, List<SelectedKLESubjectDTO> subjects) {}
	record SelectedKleMainGroupDTO(String mainGroupNumber, String title, List<SelectedKLEGroupDTO> groups) {}

	private List<SelectedKleMainGroupDTO> toSelectedMainGroupDTOs(Set<KLEMainGroup> mainGroups, Set<KLEGroup> groups) {
		return mainGroups.stream().map(mg ->
						new SelectedKleMainGroupDTO(mg.getMainGroupNumber(), mg.getTitle(), mg.getKleGroups().stream()
								.filter(groups::contains)
								.map(this::toSelectedKLEGroupDTO)
								.sorted(Comparator.comparing(SelectedKLEGroupDTO::groupNumber))
								.toList()))
				.sorted(Comparator.comparing(SelectedKleMainGroupDTO::mainGroupNumber))
				.toList();
	}
	private SelectedKLEGroupDTO toSelectedKLEGroupDTO(KLEGroup group) {
		return new SelectedKLEGroupDTO(
				group.getGroupNumber(),
				group.getTitle(),
				group.getSubjects().stream()
						.map(this::toSelectedKLESubjectDTO )
						.sorted(Comparator.comparing(SelectedKLESubjectDTO::subjectNumber))
						.toList());
	}

	private SelectedKLESubjectDTO toSelectedKLESubjectDTO(KLESubject subject) {
		return new SelectedKLESubjectDTO(
				subject.getSubjectNumber(),
				subject.getTitle(),
				subject.getPreservationCode(),
				durationToString(subject.getDurationBeforeDeletion()),
				subject.getLegalReferences().stream()
						.map(this::selectedLegalReferenceDTO)
						.collect(Collectors.toSet()));
	}

	private SelectedLegalReferenceDTO selectedLegalReferenceDTO(KLELegalReference legalReference) {
		return new SelectedLegalReferenceDTO(
				legalReference.getAccessionNumber(),
				legalReference.getTitle(),
				legalReference.getParagraph());
	}

    @RequireSuperuserOrAdministrator
    @DeleteMapping("{id}")
    @ResponseStatus(value = HttpStatus.OK)
    @Transactional
    public void registerDelete(@PathVariable final Long id) {
        final Register register = registerService.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // All related checks should be deleted along with the register
        final List<Task> tasks = taskService.findRelatedTasks(register, t -> t.getTaskType() == TaskType.CHECK);

        relationService.deleteRelatedTo(id);
        taskService.deleteAll(tasks);
        registerService.delete(register);
    }

    @GetMapping("{id}/relations/{relatedId}/{relatedType}")
    @Transactional
    public String editRelation(final Model model, @PathVariable final long id, @PathVariable final long relatedId, @PathVariable final RelationType relatedType) {
        final Register register = registerService.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final Relation relation = relationService.findRelationEntity(register, relatedId, relatedType);
        final Asset asset;
        if (relation.getRelationAType() == RelationType.ASSET) {
            asset = assetService.get(relation.getRelationAId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
        } else {
            asset = assetService.get(relation.getRelationBId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
        }
        model.addAttribute("relatableId", register.getId());
        model.addAttribute("relation", relation);
        model.addAttribute("asset", asset);
        model.addAttribute("relatedType", relatedType);
        model.addAttribute("properties", relation.getProperties().stream()
            .collect(Collectors.toMap(RelationProperty::getKey, RelationProperty::getValue)));
        return "registers/fragments/editAssetRelation";
    }

    private static void ensureEditingIsAllowed(final Register register) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream()
				.noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER))
				&& !register.getResponsibleUsers().stream().map(User::getUuid).toList().contains(SecurityUtil.getPrincipalUuid())
				&& !register.getCustomResponsibleUsers().stream().map(User::getUuid).toList().contains(SecurityUtil.getPrincipalUuid())
		) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private static List<ChoiceValue> sortChoicesNumeric(final ChoiceList gdprChoiceList) {
        return gdprChoiceList.getValues().stream()
                .sorted(Comparator.comparingInt(a -> asNumber(a.getIdentifier())))
				.toList();
    }

    private static List<ChoiceValue> sortChoicesAlpha(final ChoiceList gdprChoiceList) {
        return gdprChoiceList.getValues().stream()
                .sorted((a, b) -> a.getCaption().compareToIgnoreCase(b.getCaption()))
				.toList();
    }

	private String durationToString(final Duration duration) {
		if (duration.isZero()) {
			return "0 timer";
		}

		List<String> parts = new ArrayList<>();

		// Convert duration to total days and remaining time
		long totalDays = duration.toDays();
		Duration remainingTime = duration.minusDays(totalDays);

		// Calculate years, months, and days
		long years = totalDays / 365;
		long remainingDaysAfterYears = totalDays % 365;
		long months = remainingDaysAfterYears / 30; // Approximate months
		long days = remainingDaysAfterYears % 30;

		// Get hours from remaining time
		long hours = remainingTime.toHours();

		// Add non-zero components to the result
		if (years > 0) {
			parts.add(years + " år");
		}

		if (months > 0) {
			parts.add(months + " måneder");
		}

		if (days > 0) {
			parts.add(days + " dage");
		}

		if (hours > 0) {
			parts.add(hours + " timer");
		}

		// Join parts with commas
		return String.join(", ", parts);
	}
}

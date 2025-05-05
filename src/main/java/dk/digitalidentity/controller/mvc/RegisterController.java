package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.ConsequenceAssessmentDao;
import dk.digitalidentity.model.dto.DataProcessingDTO;
import dk.digitalidentity.model.dto.RegisterAssetRiskDTO;
import dk.digitalidentity.model.dto.RelationDTO;
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
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.Criticality;
import dk.digitalidentity.model.entity.enums.InformationObligationStatus;
import dk.digitalidentity.model.entity.enums.RegisterStatus;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskType;
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
import dk.digitalidentity.service.TaskService;
import dk.digitalidentity.service.UserService;
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

import java.util.Comparator;
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
            existing.setConfidentialityReason(assessment.getConfidentialityReason());

            existing.setIntegrityRegistered(assessment.getIntegrityRegistered());
            existing.setIntegrityOrganisation(assessment.getIntegrityOrganisation());
            existing.setIntegrityOrganisationRep(assessment.getIntegrityOrganisationRep());
            existing.setIntegrityOrganisationEco(assessment.getIntegrityOrganisationEco());
            existing.setIntegrityReason(assessment.getIntegrityReason());

            existing.setAvailabilityRegistered(assessment.getAvailabilityRegistered());
            existing.setAvailabilityOrganisation(assessment.getAvailabilityOrganisation());
            existing.setAvailabilityOrganisationRep(assessment.getAvailabilityOrganisationRep());
            existing.setAvailabilityOrganisationEco(assessment.getAvailabilityOrganisationEco());
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
                         @RequestParam(value = "criticality", required = false) final Criticality criticality,
                         @RequestParam(value = "emergencyPlanLink", required = false) final String emergencyPlanLink,
                         @RequestParam(value = "informationResponsible", required = false) final String informationResponsible,
                         @RequestParam(value = "registerRegarding", required = false) final String registerRegarding,
                         @RequestParam(required = false) final String section,
                         @RequestParam(value = "status", required = false) final RegisterStatus status) {
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
        if (emergencyPlanLink != null) {
            register.setEmergencyPlanLink(emergencyPlanLink);
        }
        if (informationResponsible != null) {
            register.setInformationResponsible(informationResponsible);
        }
        if (registerRegarding != null) {
            register.setRegisterRegarding(registerRegarding);
        }
        if (criticality != null) {
            register.setCriticality(criticality);
        }
        if (status != null) {
            register.setStatus(status);
        }
        registerService.save(register);
        return showIndex ? "redirect:/registers" : "redirect:/registers/" + id + (section != null ? "?section=" + section : "");
    }

    @Transactional
    @PostMapping("{id}/purpose")
    public String purpose(@PathVariable final Long id,
                          @RequestParam(value = "purpose", required = false) final String purpose,
                          @RequestParam(value = "gdprChoices", required = false) final Set<String> gdprChoices,
                          @RequestParam(value = "informationObligation", required = false) final InformationObligationStatus informationObligationStatus,
                          @RequestParam(value = "informationObligationDesc", required = false) final String informationObligationDesc,
                          @RequestParam(value = "consent", required = false) final String consent,
                          @RequestParam(value = "purposeNotes", required = false) final String purposeNotes) {
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
        if (informationObligationStatus != null) {
            register.setInformationObligation(informationObligationStatus);
        }
        if (informationObligationDesc != null) {
            register.setInformationObligationDesc(informationObligationDesc);
        }
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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        model.addAttribute("section", section);
        model.addAttribute("changeableRegister", (authentication.getAuthorities().stream()
            .anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER) || r.getAuthority().equals(Roles.ADMINISTRATOR)) || register.getResponsibleUsers().stream()
            .anyMatch(user -> user.getUuid().equals(SecurityUtil.getPrincipalUuid()))));
        model.addAttribute("dpChoices", dataProcessingService.getChoices());
        model.addAttribute("dataProcessing", register.getDataProcessing());
        model.addAttribute("register", register);
        model.addAttribute("assessment", assessment);
        model.addAttribute("gdprChoices", gdprChoices);
        model.addAttribute("gdprP6Choices", gdprP6Choices);
        model.addAttribute("gdprP7Choices", gdprP7Choices);
        model.addAttribute("relatedDocuments", allRelatedTo.stream()
                .filter(r -> r.getRelationType() == RelationType.DOCUMENT)
                .collect(Collectors.toList()));
        model.addAttribute("relatedTasks", allRelatedTo.stream()
                .filter(r -> r.getRelationType() == RelationType.TASK)
                .collect(Collectors.toList()));
        model.addAttribute("relatedAssets", relatedAssets);
        model.addAttribute("threatAssessments", allRelatedTo.stream()
            .filter(r -> r.getRelationType() == RelationType.THREAT_ASSESSMENT)
            .collect(Collectors.toList()));
        model.addAttribute("assetThreatAssessments", assetThreatAssessments);
        model.addAttribute("scale", new TreeMap<>(scaleService.getConsequenceScale()));
        model.addAttribute("consequenceScale", scaleService.getConsequenceNumberDescriptions());
        model.addAttribute("relatedAssetsSubSuppliers", assetSupplierMappingList);

        return "registers/view";
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
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !register.getResponsibleUsers().stream().map(User::getUuid).toList().contains(SecurityUtil.getPrincipalUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private static List<ChoiceValue> sortChoicesNumeric(final ChoiceList gdprChoiceList) {
        return gdprChoiceList.getValues().stream()
                .sorted(Comparator.comparingInt(a -> asNumber(a.getIdentifier())))
                .collect(Collectors.toList());
    }

    private static List<ChoiceValue> sortChoicesAlpha(final ChoiceList gdprChoiceList) {
        return gdprChoiceList.getValues().stream()
                .sorted((a, b) -> a.getCaption().compareToIgnoreCase(b.getCaption()))
                .collect(Collectors.toList());
    }

}

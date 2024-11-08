package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.Constants;
import dk.digitalidentity.dao.AssetMeasuresDao;
import dk.digitalidentity.dao.ChoiceDPIADao;
import dk.digitalidentity.dao.ChoiceMeasuresDao;
import dk.digitalidentity.event.AssetUpdatedEvent;
import dk.digitalidentity.integration.kitos.KitosConstants;
import dk.digitalidentity.mapping.AssetMapper;
import dk.digitalidentity.model.dto.DataProcessingDTO;
import dk.digitalidentity.model.dto.DataProcessingOversightDTO;
import dk.digitalidentity.model.dto.DataProtectionImpactDTO;
import dk.digitalidentity.model.dto.DataProtectionImpactScreeningAnswerDTO;
import dk.digitalidentity.model.dto.SaveMeasureDTO;
import dk.digitalidentity.model.dto.SaveMeasuresDTO;
import dk.digitalidentity.model.dto.ViewMeasureDTO;
import dk.digitalidentity.model.dto.ViewMeasuresDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetMeasure;
import dk.digitalidentity.model.entity.AssetOversight;
import dk.digitalidentity.model.entity.AssetSupplierMapping;
import dk.digitalidentity.model.entity.ChoiceDPIA;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceMeasure;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.DPIAReport;
import dk.digitalidentity.model.entity.DPIAResponseSection;
import dk.digitalidentity.model.entity.DPIAResponseSectionAnswer;
import dk.digitalidentity.model.entity.DPIATemplateQuestion;
import dk.digitalidentity.model.entity.DPIATemplateSection;
import dk.digitalidentity.model.entity.DataProcessingCategoriesRegistered;
import dk.digitalidentity.model.entity.DataProtectionImpactAssessmentScreening;
import dk.digitalidentity.model.entity.DataProtectionImpactScreeningAnswer;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.AssetOversightStatus;
import dk.digitalidentity.model.entity.enums.AssetStatus;
import dk.digitalidentity.model.entity.enums.ChoiceOfSupervisionModel;
import dk.digitalidentity.model.entity.enums.Criticality;
import dk.digitalidentity.model.entity.enums.DPIAAnswerPlaceholder;
import dk.digitalidentity.model.entity.enums.DataProcessingAgreementStatus;
import dk.digitalidentity.model.entity.enums.ForwardInformationToOtherSuppliers;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.RevisionInterval;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.enums.ThirdCountryTransfer;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentReportApprovalStatus;
import dk.digitalidentity.security.RequireSuperuser;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.service.AssetOversightService;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.DPIATemplateQuestionService;
import dk.digitalidentity.service.DPIATemplateSectionService;
import dk.digitalidentity.service.DataProcessingService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.ScaleService;
import dk.digitalidentity.service.SupplierService;
import dk.digitalidentity.service.TaskService;
import dk.digitalidentity.service.ThreatAssessmentService;
import dk.digitalidentity.service.model.PlaceholderInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.htmlcleaner.BrowserCompactXmlSerializer;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.base.Strings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.util.LinkHelper.linkify;

@SuppressWarnings("ClassEscapesDefinedScope")
@Slf4j
@Controller
@RequireUser
@RequestMapping("assets")
@RequiredArgsConstructor
public class AssetsController {
    private final ApplicationEventPublisher eventPublisher;
	private final RelationService relationService;
    private final ChoiceService choiceService;
	private final SupplierService supplierService;
	private final AssetMeasuresDao assetMeasuresDao;
	private final ChoiceMeasuresDao choiceMeasuresDao;
	private final ChoiceDPIADao choiceDPIADao;
    private final DataProcessingService dataProcessingService;
    private final ScaleService scaleService;
    private final ThreatAssessmentService threatAssessmentService;
    private final AssetOversightService assetOversightService;
    private final AssetService assetService;
    private final AssetMapper assetMapper;
    private final TaskService taskService;
    private final DPIATemplateSectionService dpiaTemplateSectionService;
    private final DPIATemplateQuestionService dpiaTemplateQuestionService;

	@GetMapping
	public String assetsList() {
		return "assets/index";
	}

	@GetMapping("form")
	public String form(final Model model, @RequestParam(name = "id", required = false) final Long id) {
		if (id == null) {
			model.addAttribute("asset", new Asset());
			model.addAttribute("formId", "createForm");
			model.addAttribute("formTitle", "Nyt aktiv");
		} else {
			final Asset asset = assetService.get(id)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
			model.addAttribute("asset", asset);
			model.addAttribute("formId", "editForm");
			model.addAttribute("formTitle", "Rediger aktiv");
		}
		return "assets/form";
	}

    @RequireSuperuser
	@Transactional
	@PostMapping("form")
	public String formCreate(@ModelAttribute final Asset asset) {
		asset.setAssetStatus(AssetStatus.NOT_STARTED);
		asset.setCriticality(Criticality.NON_CRITICAL);
		asset.setDataProcessingAgreementStatus(DataProcessingAgreementStatus.NO);

		final Asset newAsset = assetService.create(asset);
		return "redirect:/assets/" + newAsset.getId();
	}

    record DPIAQuestionDTO(long id, long questionResponseId, String question, String instructions, String templateAnswer, String response) {}
    record DPIASectionDTO(long id, String sectionIdentifier, long sectionResponseId, String heading, String explainer, boolean canOptOut, boolean hasOptedOutResponse, List<DPIAQuestionDTO> questions) {}
	@GetMapping("{id}")
    @Transactional
	public String view(final Model model, @PathVariable final long id) {
		final Asset asset = assetService.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        final List<Relatable> allRelatedTo = relationService.findAllRelatedTo(asset);
        final List<Relatable> relatedAssets = allRelatedTo.stream().filter(r -> r.getRelationType() == RelationType.ASSET).toList();
		final List<Relatable> registers = allRelatedTo.stream().filter(r -> r.getRelationType() == RelationType.REGISTER).toList();
		final List<Relatable> documents = allRelatedTo.stream().filter(r -> r.getRelationType() == RelationType.DOCUMENT).toList();
		final List<Relatable> precautions = allRelatedTo.stream().filter(r -> r.getRelationType() == RelationType.PRECAUTION).toList();
		final List<Relatable> relatedIncidents = allRelatedTo.stream().filter(r -> r.getRelationType() == RelationType.INCIDENT).toList();
		final List<ThreatAssessment> threatAssessments = allRelatedTo.stream()
            .filter(r -> r.getRelationType() == RelationType.THREAT_ASSESSMENT)
            .map(ThreatAssessment.class::cast)
            .collect(Collectors.toList());
        threatAssessments.sort(Comparator.comparing(Relatable::getCreatedAt).reversed());
		final List<Relatable> tasks = relationService.findAllRelatedTo(asset).stream().filter(r -> r.getRelationType() == RelationType.TASK).toList();

		final ChoiceList acceptListIdentifiers = choiceService.findChoiceList("dp-supplier-accept-list")
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						"Could not find acceptListIdentifiers Choices"));

        final ChoiceList dpiaQualityCheckList = choiceService.findChoiceList("dpia-quality-checklist")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not find dpia quality checklist"));

		// MEASURES

		final List<ChoiceMeasure> choiceMeasures = choiceMeasuresDao.findAll();
		final List<AssetMeasure> assetMeasures = assetMeasuresDao.findByAsset(asset);

		final List<ViewMeasureDTO> measures = new ArrayList<>();

		for (final ChoiceMeasure choiceMeasure : choiceMeasures) {
			final AssetMeasure assetMeasure = assetMeasures.stream().filter(m -> Objects.equals(m.getMeasure().getId(), choiceMeasure.getId())).findAny().orElse(new AssetMeasure());
			final ViewMeasureDTO measure = new ViewMeasureDTO();

			measure.setId(assetMeasure.getId());
			measure.setAnswer(assetMeasure.getAnswer());
			measure.setNote(assetMeasure.getNote());
			measure.setTask(assetMeasure.getTask());
			measure.setIdentifier(choiceMeasure.getIdentifier());
			measure.setChoice(choiceMeasure);

			measures.add(measure);
		}
		final ViewMeasuresDTO measuresForm = new ViewMeasuresDTO(0L, measures);

		// DPIA
		final List<DataProtectionImpactScreeningAnswerDTO> assetDPIADTOs = new ArrayList<>();
		final List<ChoiceDPIA> choiceDPIA = choiceDPIADao.findAll();
        for (final ChoiceDPIA choice : choiceDPIA) {
            final DataProtectionImpactScreeningAnswer defaultAnswer = new DataProtectionImpactScreeningAnswer();
            defaultAnswer.setAssessment(asset.getDpiaScreening());
            defaultAnswer.setChoice(choice);
            defaultAnswer.setAnswer(null);
            defaultAnswer.setId(0);
            final DataProtectionImpactScreeningAnswer dpiaAnswer = asset.getDpiaScreening().getDpiaScreeningAnswers().stream()
                .filter(m -> Objects.equals(m.getChoice().getId(), choice.getId()))
                .findAny().orElse(defaultAnswer);
            final DataProtectionImpactScreeningAnswerDTO dpiaDTO = new DataProtectionImpactScreeningAnswerDTO();
            dpiaDTO.setAssetId(asset.getId());
            dpiaDTO.setAnswer(dpiaAnswer.getAnswer());
            dpiaDTO.setChoice(choice);
            assetDPIADTOs.add(dpiaDTO);
        }
        final DataProtectionImpactDTO dpiaForm = DataProtectionImpactDTO.builder()
            .assetId(asset.getId())
            .optOut(asset.isDpiaOptOut())
            .questions(assetDPIADTOs)
            .consequenceLink(asset.getDpiaScreening().getConsequenceLink())
            .dpiaQuality(asset.getDpia() == null ? new HashSet<>() : asset.getDpia().getChecks())
            .build();

        if (asset.getDpia() == null) {
            DPIA dpia = new DPIA();
            dpia.setAsset(asset);
            asset.setDpia(dpia);
        }

        // Oversights
        final List<AssetOversight> oversights = new ArrayList<>(
            assetOversightService.findByAssetOrderByCreationDateDesc(asset));

		model.addAttribute("asset", asset);
		model.addAttribute("relatedAssets", relatedAssets);
		model.addAttribute("relatedIncidents", relatedIncidents);
		model.addAttribute("registers", registers);
		model.addAttribute("documents", documents);
		model.addAttribute("precautions", precautions);
		model.addAttribute("tasks", tasks);
		model.addAttribute("dataProcessing", asset.getDataProcessing());
		model.addAttribute("dpChoices", dataProcessingService.getChoices());
		model.addAttribute("acceptanceBasisChoices", acceptListIdentifiers);
        model.addAttribute("isKitos", asset.getProperties().stream().anyMatch(p -> p.getKey().equals(KitosConstants.KITOS_UUID_PROPERTY_KEY)));
        model.addAttribute("oversight", oversights.isEmpty() ? null : oversights.get(0));
        model.addAttribute("oversights", oversights);
		model.addAttribute("measuresForm", measuresForm);
        model.addAttribute("supplier", supplierService.getAll());
        model.addAttribute("dpiaQualityCheckList", dpiaQualityCheckList);
		model.addAttribute("dpiaForm", dpiaForm);
		model.addAttribute("dpiaRevisionTasks", taskService.buildDPIARelatedTasks(asset, false));
		model.addAttribute("dpiaSections", buildDPIASections(asset));
		model.addAttribute("dpiaThreatAssesments", buildDPIAThreatAssessments(asset, threatAssessments));
		model.addAttribute("dpiaReports", buildDPIAReports(asset));
		model.addAttribute("conclusion", asset.getDpia().getConclusion());
		model.addAttribute("responsibleUserNames", asset.getResponsibleUsers().stream().map(u -> u.getName() + "(" + u.getUserId() + ")").collect(Collectors.joining(", ")));
		model.addAttribute("managerNames", asset.getManagers().stream().map(u -> u.getName() + "(" + u.getUserId() + ")").collect(Collectors.joining(", ")));
		model.addAttribute("supplierName", asset.getSupplier() == null ? "" : asset.getSupplier().getName());
        model.addAttribute("defaultSendReportTo", asset.getResponsibleUsers().stream().filter(u -> StringUtils.hasLength(u.getEmail())).findFirst().orElse(null));

        // threat assessments
        model.addAttribute("threatAssessments", threatAssessments);
        final boolean threatExists = !threatAssessments.isEmpty();
        model.addAttribute("threatExists", threatExists);
        if (threatExists) {
            final ThreatAssessment newestThreatAssessment = threatAssessments.get(0);
            model.addAttribute("risk", newestThreatAssessment);
            model.addAttribute("reversedScale", scaleService.getConsequenceScale().keySet().stream().sorted(Collections.reverseOrder()).collect(Collectors.toList()));
            model.addAttribute("riskScoreColorMap", scaleService.getScaleRiskScoreColorMap());
            model.addAttribute("riskProfiles", threatAssessmentService.buildRiskProfileDTOs(newestThreatAssessment));
            model.addAttribute("riskScoreColorMap", scaleService.getScaleRiskScoreColorMap());
            model.addAttribute("unfinishedTasks", taskService.buildRelatedTasks(threatAssessments, false));
        }

		return "assets/view";
	}

    @RequireSuperuser
    @DeleteMapping("{id}")
    @ResponseStatus(value = HttpStatus.OK)
    @Transactional
    public void assetDelete(@PathVariable final Long id) {
        final Asset asset = assetService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // All related checks should be deleted along with the asset
        final List<Task> tasks = taskService.findRelatedTasks(asset, t -> t.getTaskType() == TaskType.CHECK);
        relationService.deleteRelatedTo(id);
        taskService.deleteAll(tasks);
        asset.getSuppliers().clear();
        assetService.deleteById(asset);
    }

	@Transactional
	@PostMapping("dataprocessing")
	public String dataprocessing(@Valid @ModelAttribute final DataProcessingDTO body) {
		final Asset asset = assetService.get(body.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(authentication.getPrincipal().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        dataProcessingService.update(asset.getDataProcessing(), body);
        final List<DataProcessingCategoriesRegistered> registeredCategories = asset.getDataProcessing().getRegisteredCategories();
        if (asset.getTia().getRegisteredCategories() == null && registeredCategories != null) {
            asset.getTia().setRegisteredCategories(registeredCategories.stream()
                .map(DataProcessingCategoriesRegistered::getPersonCategoriesRegisteredIdentifier)
                .collect(Collectors.toSet()));
        }
        if (asset.getTia().getInformationTypes() == null && registeredCategories != null) {
            asset.getTia().setInformationTypes(registeredCategories.stream()
                .flatMap(d -> d.getPersonCategoriesInformationIdentifiers().stream())
                .collect(Collectors.toSet()));
        }
		return "redirect:/assets/" + body.getId();
	}

    @Transactional
    @PostMapping("measures")
    public String measures(@ModelAttribute final SaveMeasuresDTO measuresForm) {
        final Asset asset = assetService.get(measuresForm.getAssetId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(authentication.getPrincipal().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        for (final SaveMeasureDTO answer : measuresForm.getMeasures()) {
            AssetMeasure existing = assetMeasuresDao.findByAssetAndMeasureIdentifier(asset, answer.getIdentifier()).orElse(null);
            if (existing == null) {
                existing = new AssetMeasure();
                existing.setAsset(asset);
                existing.setMeasure(choiceMeasuresDao.findByIdentifier(answer.getIdentifier()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
            }
            existing.setAnswer(answer.getAnswer());
            existing.setNote(answer.getNote());
            existing.setTask(answer.getTask());
            assetMeasuresDao.save(existing);
        }
        return "redirect:/assets/" + measuresForm.getAssetId();
    }

    @Transactional
    @PostMapping("dpia")
    public String dpia(@ModelAttribute final DataProtectionImpactDTO dpiaForm) {
        final Asset asset = assetService.get(dpiaForm.getAssetId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(authentication.getPrincipal().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        asset.setDpiaOptOut(dpiaForm.isOptOut());
        final DataProtectionImpactAssessmentScreening dpiaScreening = asset.getDpiaScreening();
        for (final DataProtectionImpactScreeningAnswerDTO question : dpiaForm.getQuestions()) {
            final DataProtectionImpactScreeningAnswer foundAnswer = dpiaScreening.getDpiaScreeningAnswers().stream()
                .filter(a -> a.getChoice().getIdentifier().equalsIgnoreCase(question.getChoice().getIdentifier()))
                .findFirst().orElseGet(() -> {
                    final DataProtectionImpactScreeningAnswer newAnswer = DataProtectionImpactScreeningAnswer.builder()
                        .assessment(dpiaScreening)
                        .choice(choiceDPIADao.findByIdentifier(question.getChoice().getIdentifier()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)))
                        .build();
                    dpiaScreening.getDpiaScreeningAnswers().add(newAnswer);
                    return newAnswer;
                });
            foundAnswer.setAnswer(question.getAnswer());
        }
        dpiaScreening.setConsequenceLink(dpiaForm.getConsequenceLink());

        if (asset.getDpia() == null) {
            DPIA dpia = new DPIA();
            dpia.setAsset(asset);
            asset.setDpia(dpia);
        }

        asset.getDpia().setChecks(dpiaForm.getDpiaQuality());

        return "redirect:/assets/" + dpiaForm.getAssetId();
    }


    @Transactional
    @PostMapping("edit")
    public String formEdit(@ModelAttribute final Asset asset) {
        final Asset existingAsset = assetService.get(asset.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(authentication.getPrincipal().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        existingAsset.getManagers().clear();
        existingAsset.getManagers().addAll(asset.getManagers());

        if(!Objects.isNull(asset.getSupplier())) {
            existingAsset.setSupplier(asset.getSupplier());
        }
        existingAsset.setAssetType(asset.getAssetType());
        existingAsset.setProductLink(asset.getProductLink());
        existingAsset.setCriticality(asset.getCriticality());

        existingAsset.setDescription(asset.getDescription());
        existingAsset.setSociallyCritical(asset.isSociallyCritical());
        existingAsset.setEmergencyPlanLink(asset.getEmergencyPlanLink());
        existingAsset.setReEstablishmentPlanLink(asset.getReEstablishmentPlanLink());
        existingAsset.setContractLink(asset.getContractLink());
        existingAsset.setContractDate(asset.getContractDate());
        existingAsset.setContractTermination(asset.getContractTermination());
        existingAsset.setTerminationNotice(asset.getTerminationNotice());
        existingAsset.setArchive(asset.isArchive());
        existingAsset.setAssetStatus(asset.getAssetStatus());
        existingAsset.setResponsibleUsers(asset.getResponsibleUsers());

        eventPublisher.publishEvent(AssetUpdatedEvent.builder()
                .asset(assetMapper.toEO(existingAsset))
            .build());

        return "redirect:/assets/" + existingAsset.getId();
    }

    @GetMapping("subsupplier")
	public String subsupplierForm(final Model model, @RequestParam(name = "id", required = false) final Long id, @RequestParam(name = "asset", required = true) final Long assetId) {
		final ChoiceList acceptanceBasisChoices = choiceService.findChoiceList("dp-supplier-accept-list")
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						"Could not find AcceptanceBasis Choices"));

		model.addAttribute("allSuppliers", supplierService.getAll());

		if (id == null) {
			final Asset asset = assetService.get(assetId)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
			model.addAttribute("assetId", asset.getId());
			model.addAttribute("subsupplier", new AssetSupplierMapping());
			model.addAttribute("formId", "subSupplierForm");
			model.addAttribute("formTitle", "Ny underleverandør");
			model.addAttribute("choices", acceptanceBasisChoices);
		} else {
			final Asset asset = assetService.get(assetId)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
			final AssetSupplierMapping subsupplier = asset.getSuppliers().stream().filter(s -> Objects.equals(s.getId(), id)).findAny()
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

			model.addAttribute("assetId", asset.getId());
			model.addAttribute("subsupplier", subsupplier);
			model.addAttribute("formId", "subSupplierForm");
			model.addAttribute("formTitle", "Rediger underleverandør");
			model.addAttribute("choices", acceptanceBasisChoices);
		}
		return "assets/fragments/subsupplier";
	}

	record AssetSupplierDTO(long id, long assetId, long supplier, String service, ThirdCountryTransfer thirdCountryTransfer, String acceptanceBasis) {}

    @Transactional
	@PostMapping("subsupplier")
	public String subsupplierCreateOrEdit(@Valid @ModelAttribute final AssetSupplierDTO body) {
		final Asset asset = assetService.get(body.assetId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(authentication.getPrincipal().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
		final Optional<AssetSupplierMapping> subSupplier = asset.getSuppliers().stream().filter(s -> Objects.equals(s.getId(), body.id)).findAny();
		final Supplier supplier = supplierService.get(body.supplier).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		if (subSupplier.isPresent()) {
			//Edit
			subSupplier.get().setSupplier(supplier);
			subSupplier.get().setAcceptanceBasis(body.acceptanceBasis);
			subSupplier.get().setService(body.service);
			subSupplier.get().setThirdCountryTransfer(body.thirdCountryTransfer);
		} else {
			//Create
			final AssetSupplierMapping newSubsupplier = new AssetSupplierMapping();
			newSubsupplier.setAsset(asset);
			newSubsupplier.setSupplier(supplier);
			newSubsupplier.setAcceptanceBasis(body.acceptanceBasis);
			newSubsupplier.setService(body.service);
			newSubsupplier.setThirdCountryTransfer(body.thirdCountryTransfer);
			asset.getSuppliers().add(newSubsupplier);
		}
		return "redirect:/assets/" + asset.getId();
	}


    @Transactional
    @PostMapping("oversight")
    public String oversightSettings(@Valid @ModelAttribute final DataProcessingOversightDTO body) {
        final Asset asset = assetService.get(body.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(authentication.getPrincipal().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        asset.setDataProcessingAgreementStatus(body.getDataProcessingAgreementStatus());
        asset.setDataProcessingAgreementLink(linkify(Strings.emptyToNull(body.getDataProcessingAgreementLink())));
        asset.setDataProcessingAgreementDate(body.getDataProcessingAgreementDate());
        asset.setSupervisoryModel(body.getSupervisoryModel());
        asset.setNextInspection(body.getNextInspection());
        if (body.getNextInspectionDate() == null) {
            asset.setNextInspectionDate(assetService.getNextInspectionByInterval(asset, LocalDate.now()));
        } else {
            asset.setNextInspectionDate(body.getNextInspectionDate());
        }
        asset.setOversightResponsibleUser(body.getOversightResponsibleUser());

        dataProcessingService.createOrUpdateAssociatedOversightCheck(asset);
        return "redirect:/assets/" + asset.getId();
    }


    record AssetOversightDTO (long id, long assetId, User responsibleUser, ChoiceOfSupervisionModel supervisionModel, String conclusion, String dbsLink, String internalDocumentationLink, AssetOversightStatus status, @DateTimeFormat(pattern = "dd/MM-yyyy") LocalDate creationDate, @DateTimeFormat(pattern = "dd/MM-yyyy") LocalDate newInspectionDate, String redirect){
    }
    @Transactional
    @PostMapping("oversight/edit")
    public String oversightCreateOrEdit(@Valid @ModelAttribute final AssetOversightDTO dto) {
        final Asset asset = assetService.get(dto.assetId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(authentication.getPrincipal().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        final Optional<AssetOversight> oversight = asset.getAssetOversights().stream().filter(s -> Objects.equals(s.getId(), dto.id)).findAny();

        if (oversight.isPresent()) {
            oversight.get().setCreationDate(dto.creationDate);
            oversight.get().setResponsibleUser(dto.responsibleUser);
            oversight.get().setSupervisionModel(dto.supervisionModel);
            oversight.get().setConclusion(dto.conclusion);
            oversight.get().setStatus(dto.status);
            oversight.get().setDbsLink(linkify(Strings.emptyToNull(dto.dbsLink)));
            oversight.get().setInternalDocumentationLink(linkify(Strings.emptyToNull(dto.internalDocumentationLink)));

            if (dto.newInspectionDate == null) {
                asset.setNextInspectionDate(assetService.getNextInspectionByInterval(asset, oversight.get().getCreationDate()));
            } else {
                asset.setNextInspectionDate(dto.newInspectionDate);
            }
            oversight.get().setNewInspectionDate(asset.getNextInspectionDate());
        } else {
            //Create
            AssetOversight newOversight = new AssetOversight();
            newOversight.setAsset(asset);
            newOversight.setConclusion(dto.conclusion);
            if (Objects.isNull(dto.creationDate)) {
                newOversight.setCreationDate(LocalDate.now());
            } else {
                newOversight.setCreationDate(dto.creationDate);
            }
            newOversight.setResponsibleUser(dto.responsibleUser);
            newOversight.setStatus(dto.status);
            newOversight.setSupervisionModel(dto.supervisionModel);
            newOversight.setDbsLink(linkify(Strings.emptyToNull(dto.dbsLink)));
            newOversight.setInternalDocumentationLink(linkify(Strings.emptyToNull(dto.internalDocumentationLink)));

            if (dto.newInspectionDate == null) {
                asset.setNextInspectionDate(assetService.getNextInspectionByInterval(asset, newOversight.getCreationDate()));
            } else {
                asset.setNextInspectionDate(dto.newInspectionDate);
            }
            newOversight.setNewInspectionDate(asset.getNextInspectionDate());
            final AssetOversight attachedOversight = assetOversightService.create(newOversight);
            assetOversightService.createAssociatedCheck(attachedOversight);
            asset.getAssetOversights().add(attachedOversight);
        }

        return dto.redirect.equals("assets") ? "redirect:/assets/" + asset.getId() : "redirect:/suppliers/" + asset.getSupplier().getId();
    }

    @GetMapping("oversight/{entityId}/{type}")
    public String oversightForm(final Model model, final @PathVariable("entityId") Long entityId, @PathVariable("type") final String type, @RequestParam(name = "id", required = false) final Long id) {
        if(Objects.isNull(entityId)){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id blev ikke sendt med");
        }

        if(type.equals("asset")) {
            final Asset asset = assetService.get(entityId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Det angivne id for aktiviteten findes ikke")
            );

            if (id == null) {
                model.addAttribute("assetId", asset.getId());
                model.addAttribute("oversight", new AssetOversightDTO(0, 0, new User(), ChoiceOfSupervisionModel.SWORN_STATEMENT, "", "", "", AssetOversightStatus.RED, LocalDate.now(), LocalDate.now(), "assets"));
                model.addAttribute("inspectionType", asset.getNextInspection());
            } else {
                final AssetOversight assetOversight = asset.getAssetOversights().stream().filter(s -> Objects.equals(s.getId(), id)).findAny().orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Det angivne id for oversight findes ikke")
                );
                model.addAttribute("assetId", asset.getId());
                model.addAttribute("oversight", new AssetOversightDTO(assetOversight.getId(), entityId, assetOversight.getResponsibleUser(), assetOversight.getSupervisionModel(), assetOversight.getConclusion(), assetOversight.getDbsLink(), assetOversight.getInternalDocumentationLink(), assetOversight.getStatus(), assetOversight.getCreationDate(), assetOversight.getNewInspectionDate(), "assets"));
                model.addAttribute("inspectionType", asset.getNextInspection());
            }

            return "assets/fragments/oversightModal";
        }
        if(type.equals("supplier")) {
            final Supplier supplier = supplierService.get(entityId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Det angivne id findes ikke"));

            if (id == null) {
                model.addAttribute("oversight", new AssetOversightDTO(0, 0, new User(), ChoiceOfSupervisionModel.SWORN_STATEMENT, "", "", "", AssetOversightStatus.RED, LocalDate.now(), LocalDate.now(), "suppliers"));
                model.addAttribute("supplier", supplier);
                model.addAttribute("inspectionType", null);
                model.addAttribute("assetId", null);
                model.addAttribute("supplierAssets", supplier.getAssets());
            } else {
                final AssetOversight assetOversight = assetOversightService.findById(id).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Det angivne id for oversight findes ikke")
                );
                model.addAttribute("oversight", new AssetOversightDTO(assetOversight.getId(), entityId, assetOversight.getResponsibleUser(), assetOversight.getSupervisionModel(), assetOversight.getConclusion(), assetOversight.getDbsLink(), assetOversight.getInternalDocumentationLink(), assetOversight.getStatus(), assetOversight.getCreationDate(), assetOversight.getNewInspectionDate(), "suppliers"));
                model.addAttribute("supplier", supplier);
                model.addAttribute("inspectionType", null);
                model.addAttribute("assetId", assetOversight.getAsset().getId());
                model.addAttribute("supplierAssets", supplier.getAssets());
            }
            return "assets/fragments/oversightModal";
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "typen fandtes ikke: understøttede er 'asset' og 'supplier'");
    }

    @Transactional
    @PostMapping("tia")
    public String tia(@ModelAttribute final Asset asset) {
        final Asset existingAsset = assetService.get(asset.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(authentication.getPrincipal().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        existingAsset.getTia().setForwardInformationToOtherSuppliers(asset.getTia().getForwardInformationToOtherSuppliers());
        existingAsset.getTia().setForwardInformationToOtherSuppliersDetail(asset.getTia().getForwardInformationToOtherSuppliersDetail());

        if (existingAsset.getTia().getForwardInformationToOtherSuppliers() != ForwardInformationToOtherSuppliers.YES) {
            existingAsset.getTia().setForwardInformationToOtherSuppliersDetail(null);
        }

        existingAsset.getTia().setAccessType(asset.getTia().getAccessType());
        existingAsset.getTia().setAssessment(asset.getTia().getAssessment());
        existingAsset.getTia().setConclusion(asset.getTia().getConclusion());
        existingAsset.getTia().setExpectedTransferDuration(asset.getTia().getExpectedTransferDuration());
        existingAsset.getTia().setContractualSecurityMeasures(asset.getTia().getContractualSecurityMeasures());
        existingAsset.getTia().setTechnicalSecurityMeasures(asset.getTia().getTechnicalSecurityMeasures());
        existingAsset.getTia().setOrganizationalSecurityMeasures(asset.getTia().getOrganizationalSecurityMeasures());
        existingAsset.getTia().setRegisteredCategories(asset.getTia().getRegisteredCategories());
        existingAsset.getTia().setInformationTypes(asset.getTia().getInformationTypes());

        existingAsset.getTia().setTransferCaseDescription(asset.getTia().getTransferCaseDescription());
        return "redirect:/assets/" + existingAsset.getId();
    }

    @GetMapping("dpia/schema")
    public String dpiaSchema(final Model model) {
        return "dpia/schema";
    }

    record TemplateSectionDTO(long id, Long sortKey, String identifier, String heading, String explainer, boolean canOptOut, boolean hasOptedOut, List<DPIATemplateQuestion> dpiaTemplateQuestions, long minQuestionSortKey, long maxQuestionSortKey) {}
    @GetMapping("dpia/schema/fragment")
    @Transactional
    public String editRelation(final Model model) {
        List<DPIATemplateSection> templateSections = dpiaTemplateSectionService.findAll().stream()
            .sorted(Comparator.comparing(DPIATemplateSection::getSortKey))
            .collect(Collectors.toList());

        List<TemplateSectionDTO> templateSectionDTOS = new ArrayList<>();
        for (DPIATemplateSection section : templateSections) {
            List<DPIATemplateQuestion> questions = section.getDpiaTemplateQuestions().stream().filter(q -> !q.isDeleted()).sorted(Comparator.comparing(DPIATemplateQuestion::getSortKey)).collect(Collectors.toList());
            long minSortKey = questions.get(0).getSortKey();
            long maxSortKey = questions.get(questions.size() - 1).getSortKey();
            TemplateSectionDTO dto = new TemplateSectionDTO(section.getId(), section.getSortKey(), section.getIdentifier(), section.getHeading(),
                section.getExplainer(), section.isCanOptOut(), section.isHasOptedOut(), questions, minSortKey, maxSortKey);
            templateSectionDTOS.add(dto);
        }

        model.addAttribute("templateSections", templateSectionDTOS);
        model.addAttribute("minSectionSortKey", templateSections.get(0).getSortKey());
        model.addAttribute("maxSectionSortKey", templateSections.get(templateSections.size() - 1).getSortKey());
        return "dpia/fragments/dpiaTemplateFragment";
    }

    record DPIATemplateQuestionForm(Long id, String title, String instructions, Long sectionId) {}
    @GetMapping("dpia/schema/question/form")
    public String questionForm(final Model model, @RequestParam(name = "id", required = false) final Long id) {
        model.addAttribute("action", "schema/question/form");
        if (id == null) {
            model.addAttribute("question", new DPIATemplateQuestionForm(null, "", "", null));
            model.addAttribute("formId", "createForm");
            model.addAttribute("instructionsId", "createInstructions");
            model.addAttribute("formTitle", "Nyt spørgsmål");
        } else {
            final DPIATemplateQuestion question = dpiaTemplateQuestionService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            model.addAttribute("question", new DPIATemplateQuestionForm(question.getId(), question.getQuestion(), question.getInstructions(), question.getDpiaTemplateSection().getId()));
            model.addAttribute("formId", "editForm");
            model.addAttribute("instructionsId", "editInstructions");
            model.addAttribute("formTitle", "Rediger spørgsmål");
        }

        List<DPIATemplateSection> templateSections = dpiaTemplateSectionService.findAll().stream()
            .sorted(Comparator.comparing(DPIATemplateSection::getSortKey))
            .collect(Collectors.toList());
        model.addAttribute("sections", templateSections);

        return "dpia/fragments/questionForm";
    }

    @RequireSuperuser
    @PostMapping("dpia/schema/question/form")
    public String formPost(@ModelAttribute final DPIATemplateQuestionForm dpiaTemplateQuestionForm) throws IOException {
        DPIATemplateSection section = dpiaTemplateSectionService.findById(dpiaTemplateQuestionForm.sectionId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        if (!StringUtils.hasLength(dpiaTemplateQuestionForm.title)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (dpiaTemplateQuestionForm.id != null) {
            DPIATemplateQuestion dpiaTemplateQuestion = dpiaTemplateQuestionService.findById(dpiaTemplateQuestionForm.id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            dpiaTemplateQuestion.setQuestion(dpiaTemplateQuestionForm.title);
            dpiaTemplateQuestion.setInstructions(dpiaTemplateQuestionForm.instructions);

            if (dpiaTemplateQuestion.getDpiaTemplateSection().getId() != section.getId()) {
                long maxSortKey = section.getDpiaTemplateQuestions().stream().max(Comparator.comparing(q -> q.getSortKey())).get().getSortKey();
                dpiaTemplateQuestion.setDpiaTemplateSection(section);
                dpiaTemplateQuestion.setSortKey(maxSortKey);
            }

            dpiaTemplateQuestionService.save(dpiaTemplateQuestion);
        } else {

            long maxSortKey = section.getDpiaTemplateQuestions().stream().max(Comparator.comparing(q -> q.getSortKey())).get().getSortKey();

            DPIATemplateQuestion dpiaTemplateQuestion = new DPIATemplateQuestion();
            dpiaTemplateQuestion.setQuestion(dpiaTemplateQuestionForm.title);
            dpiaTemplateQuestion.setInstructions(toXHTML(dpiaTemplateQuestionForm.instructions));
            dpiaTemplateQuestion.setDpiaTemplateSection(section);
            dpiaTemplateQuestion.setSortKey(maxSortKey);

            section.getDpiaTemplateQuestions().add(dpiaTemplateQuestion);
            dpiaTemplateSectionService.save(section);

        }
        return "redirect:/assets/dpia/schema";
    }

    record RevisionFormDTO(@DateTimeFormat(pattern = "dd/MM-yyyy") LocalDate nextRevision, RevisionInterval revisionInterval) {}
    @GetMapping("{id}/revision")
    public String revisionForm(final Model model, @PathVariable final long id) {
        final Asset asset = assetService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        assetService.updateNextRevisionAssociatedTask(asset);
        model.addAttribute("asset", asset);
        model.addAttribute("RevisionFormDTO", new RevisionFormDTO(asset.getDpia().getNextRevision(), asset.getDpia().getRevisionInterval()));
        return "assets/fragments/revisionIntervalForm";
    }


    @PostMapping("{id}/revision")
    @Transactional
    public String postRevisionForm(@ModelAttribute final RevisionFormDTO revisionFormDTO, @PathVariable final long id) {
        final Asset asset = assetService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(authentication.getPrincipal().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (asset.getDpia() == null) {
            DPIA dpia = new DPIA();
            dpia.setAsset(asset);
            asset.setDpia(dpia);
        }

        asset.getDpia().setRevisionInterval(revisionFormDTO.revisionInterval);
        asset.getDpia().setNextRevision(revisionFormDTO.nextRevision);
        assetService.createOrUpdateAssociatedCheck(asset);
        return "redirect:/assets/" + id;
    }

    private List<DPIASectionDTO> buildDPIASections(Asset asset) {
        List<DPIASectionDTO> sections = new ArrayList<>();
        List<DPIATemplateSection> allSections = dpiaTemplateSectionService.findAll().stream()
            .sorted(Comparator.comparing(DPIATemplateSection::getSortKey))
            .collect(Collectors.toList());

        // needed dataprocessing fields
        PlaceholderInfo placeholderInfo = assetService.getDPIAResponsePlaceholderInfo(asset);

        for (DPIATemplateSection templateSection : allSections) {
            if (templateSection.isHasOptedOut()) {
                continue;
            }

            List<DPIAQuestionDTO> questionDTOS = new ArrayList<>();
            DPIAResponseSection matchSection = asset.getDpia().getDpiaResponseSections().stream().filter(s -> s.getDpiaTemplateSection().getId() == templateSection.getId()).findAny().orElse(null);
            List<DPIATemplateQuestion> questions = templateSection.getDpiaTemplateQuestions().stream()
                .sorted(Comparator.comparing(DPIATemplateQuestion::getSortKey))
                .collect(Collectors.toList());
            for (DPIATemplateQuestion templateQuestion : questions) {
                DPIAResponseSectionAnswer matchAnswer = matchSection == null ? null : matchSection.getDpiaResponseSectionAnswers().stream().filter(s -> s.getDpiaTemplateQuestion().getId() == templateQuestion.getId()).findAny().orElse(null);

                String templateAnswer = templateQuestion.getAnswerTemplate() == null ? "" : templateQuestion.getAnswerTemplate();
                templateAnswer = templateAnswer
                    .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_DATA_WHO.getPlaceholder(), placeholderInfo.getSelectedAccessWhoTitles())
                    .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_DATA_HOW_MANY.getPlaceholder(), placeholderInfo.getSelectedAccessCountTitle())
                    .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_DATA_TYPES.getPlaceholder(), String.join(", ", placeholderInfo.getPersonalDataTypesTitles()))
                    .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_DATA_TYPES_FREETEXT.getPlaceholder(), placeholderInfo.getTypesOfPersonalInformationFreetext())
                    .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_CATEGORIES_OF_REGISTERED.getPlaceholder(), String.join(", ", placeholderInfo.getCategoriesOfRegisteredTitles()))
                    .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_HOW_LONG.getPlaceholder(), placeholderInfo.getHowLongTitle())
                    .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_DELETE_LINK.getPlaceholder(), asset.getDataProcessing().getDeletionProcedureLink() == null ? "" : "<a href=\"" + asset.getDataProcessing().getDeletionProcedureLink() + "\">" + asset.getDataProcessing().getDeletionProcedureLink() + "</a>");

                if (matchAnswer == null) {
                    questionDTOS.add(new DPIAQuestionDTO(templateQuestion.getId(), 0, templateQuestion.getQuestion(), templateQuestion.getInstructions(), templateAnswer, ""));
                } else {
                    questionDTOS.add(new DPIAQuestionDTO(templateQuestion.getId(), matchAnswer.getId(), templateQuestion.getQuestion(), templateQuestion.getInstructions(), templateAnswer, matchAnswer.getResponse()));
                }
            }

            if (matchSection == null) {
                sections.add(new DPIASectionDTO(templateSection.getId(), templateSection.getIdentifier(), 0, templateSection.getHeading(), templateSection.getExplainer(), templateSection.isCanOptOut(), false, questionDTOS));
            } else {
                sections.add(new DPIASectionDTO(templateSection.getId(), templateSection.getIdentifier(), matchSection.getId(), templateSection.getHeading(), templateSection.getExplainer(), templateSection.isCanOptOut(), !matchSection.isSelected(), questionDTOS));
            }

        }
        return sections;
    }

    record DPIAThreatAssessmentDTO(boolean selected, long threatAssessmentId, String threatAssessmentName, String date, boolean signed) {}
    private List<DPIAThreatAssessmentDTO> buildDPIAThreatAssessments(Asset asset, List<ThreatAssessment> threatAssessments) {
        List<DPIAThreatAssessmentDTO> result = new ArrayList<>();
        Set<String> selectedThreatAssessments = asset.getDpia().getCheckedThreatAssessmentIds() == null ? new HashSet<>() : Arrays.stream(asset.getDpia().getCheckedThreatAssessmentIds().split(",")).collect(Collectors.toSet());
        for (ThreatAssessment threatAssessment : threatAssessments) {
            boolean selected = selectedThreatAssessments.contains(threatAssessment.getId().toString());
            String date = threatAssessment.getCreatedAt().format(Constants.DK_DATE_FORMATTER);
            DPIAThreatAssessmentDTO dto = new DPIAThreatAssessmentDTO(selected, threatAssessment.getId(), threatAssessment.getName(), date, threatAssessment.getThreatAssessmentReportApprovalStatus().equals(ThreatAssessmentReportApprovalStatus.SIGNED));
            result.add(dto);
        }
        return result;
    }

    record DPIAReportDTO(long s3DocumentId, String approverName, String status, String date) {}
    private List<DPIAReportDTO> buildDPIAReports(Asset asset) {
        List<DPIAReportDTO> result = new ArrayList<>();
        final List<DPIAReport> sortedDpiaReports = asset.getDpia().getDpiaReports();
        sortedDpiaReports.sort(Comparator.comparing(s -> s.getDpiaReportS3Document().getTimestamp()));
        for (DPIAReport sortedDpiaReport : sortedDpiaReports) {
            String date = sortedDpiaReport.getDpiaReportS3Document().getTimestamp().format(Constants.DK_DATE_FORMATTER);
            result.add(new DPIAReportDTO(sortedDpiaReport.getDpiaReportS3Document().getId(), sortedDpiaReport.getReportApproverName(), sortedDpiaReport.getDpiaReportApprovalStatus().getMessage(), date));
        }
        return result;
    }

    /**
     * editor does not generate valid XHTML. At least the <br/> and <img/> tags are not closed,
     * so we need to close them, otherwise our PDF processing will fail.
     */
    private String toXHTML(String html) throws IOException {
        CleanerProperties properties = new CleanerProperties();
        properties.setOmitXmlDeclaration(true);
        TagNode tagNode = new HtmlCleaner(properties).clean(html);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new BrowserCompactXmlSerializer(properties).writeToStream(tagNode, bos);

        return (new String(bos.toByteArray(), Charset.forName("UTF-8")));
    }

}

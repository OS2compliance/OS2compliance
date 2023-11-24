package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.AssetMeasuresDao;
import dk.digitalidentity.dao.ChoiceDPIADao;
import dk.digitalidentity.dao.ChoiceMeasuresDao;
import dk.digitalidentity.dao.SupplierDao;
import dk.digitalidentity.integration.kitos.KitosConstants;
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
import dk.digitalidentity.model.entity.DataProcessingCategoriesRegistered;
import dk.digitalidentity.model.entity.DataProtectionImpactAssessment;
import dk.digitalidentity.model.entity.DataProtectionImpactScreeningAnswer;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.AssetOversightStatus;
import dk.digitalidentity.model.entity.enums.AssetStatus;
import dk.digitalidentity.model.entity.enums.ChoiceOfSupervisionModel;
import dk.digitalidentity.model.entity.enums.Criticality;
import dk.digitalidentity.model.entity.enums.DataProcessingAgreementStatus;
import dk.digitalidentity.model.entity.enums.ForwardInformationToOtherSuppliers;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.ThirdCountryTransfer;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.AssetOversightService;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.DataProcessingService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.RiskService;
import dk.digitalidentity.service.ScaleService;
import dk.digitalidentity.service.TaskService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequireUser
@RequestMapping("assets")
public class AssetsController {
	@Autowired
	private RelationService relationService;
    @Autowired
    private ChoiceService choiceService;
	@Autowired
	private SupplierDao supplierDao;
	@Autowired
	private AssetMeasuresDao assetMeasuresDao;
	@Autowired
	private ChoiceMeasuresDao choiceMeasuresDao;
	@Autowired
	private ChoiceDPIADao choiceDPIADao;
    @Autowired
    private DataProcessingService dataProcessingService;
    @Autowired
    private ScaleService scaleService;
    @Autowired
    private RiskService riskService;
    @Autowired
    private AssetOversightService assetOversightService;
    @Autowired
    private AssetService assetService;
    @Autowired
    private TaskService taskService;


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

	@Transactional
	@PostMapping("form")
	public String formCreate(@ModelAttribute final Asset asset) {
		asset.setAssetStatus(AssetStatus.NOT_STARTED);
		asset.setCriticality(Criticality.NON_CRITICAL);
		asset.setDataProcessingAgreementStatus(DataProcessingAgreementStatus.NO);

		final Asset newAsset = assetService.create(asset);
		return "redirect:/assets/" + newAsset.getId();
	}

	@GetMapping("{id}")
    @Transactional
	public String view(final Model model, @PathVariable final long id) {
		final Asset asset = assetService.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        final List<Relatable> allRelatedTo = relationService.findAllRelatedTo(asset);
        final List<Relatable> relatedAssets = allRelatedTo.stream().filter(r -> r.getRelationType() == RelationType.ASSET).toList();
		final List<Relatable> registers = allRelatedTo.stream().filter(r -> r.getRelationType() == RelationType.REGISTER).toList();
		final List<Relatable> documents = allRelatedTo.stream().filter(r -> r.getRelationType() == RelationType.DOCUMENT).toList();
		final List<ThreatAssessment> threatAssessments = allRelatedTo.stream()
            .filter(r -> r.getRelationType() == RelationType.THREAT_ASSESSMENT)
            .map(ThreatAssessment.class::cast)
            .collect(Collectors.toList());
		final List<Relatable> tasks = relationService.findAllRelatedTo(asset).stream().filter(r -> r.getRelationType() == RelationType.TASK).toList();

		final ChoiceList acceptListIdentifiers = choiceService.findChoiceList("dp-supplier-accept-list")
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						"Could not find acceptListIdentifiers Choices"));

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
            defaultAnswer.setAssessment(asset.getDpia());
            defaultAnswer.setChoice(choice);
            defaultAnswer.setAnswer("dpia-dont-know");
            defaultAnswer.setId(0);
            final DataProtectionImpactScreeningAnswer dpiaAnswer = asset.getDpia().getDpiaScreeningAnswers().stream()
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
            .questions(assetDPIADTOs)
            .answerA(asset.getDpia().getAnswerA())
            .answerB(asset.getDpia().getAnswerB())
            .answerC(asset.getDpia().getAnswerC())
            .answerD(asset.getDpia().getAnswerD())
            .conclusion(asset.getDpia().getConclusion())
            .consequenceLink(asset.getDpia().getConsequenceLink())
            .build();

        // Oversights
        final List<AssetOversight> oversights = new ArrayList<>(
            assetOversightService.findByAssetOrderByCreationDateDesc(asset));

		model.addAttribute("asset", asset);
		model.addAttribute("relatedAssets", relatedAssets);
		model.addAttribute("registers", registers);
		model.addAttribute("documents", documents);
		model.addAttribute("tasks", tasks);
		model.addAttribute("dataProcessing", asset.getDataProcessing());
		model.addAttribute("dpChoices", dataProcessingService.getChoices());
		model.addAttribute("acceptanceBasisChoices", acceptListIdentifiers);
        model.addAttribute("isKitos", asset.getProperties().stream().anyMatch(p -> p.getKey().equals(KitosConstants.KITOS_UUID_PROPERTY_KEY)));
        model.addAttribute("oversight", oversights.isEmpty() ? null : oversights.get(0));
        model.addAttribute("oversights", oversights);
		model.addAttribute("measuresForm", measuresForm);
        model.addAttribute("supplier", supplierDao.findAll());

		model.addAttribute("dpiaForm", dpiaForm);

        // threat assessments
        threatAssessments.sort(Comparator.comparing(Relatable::getCreatedAt).reversed());
        model.addAttribute("threatAssessments", threatAssessments);
        final boolean threatExists = !threatAssessments.isEmpty();
        model.addAttribute("threatExists", threatExists);
        if (threatExists) {
            final ThreatAssessment newestThreatAssessment = threatAssessments.get(0);
            model.addAttribute("risk", newestThreatAssessment);
            model.addAttribute("reversedScale", scaleService.getScale().keySet().stream().sorted(Collections.reverseOrder()).collect(Collectors.toList()));
            model.addAttribute("riskScoreColorMap", scaleService.getScaleRiskScoreColorMap());
            model.addAttribute("riskProfiles", riskService.buildRiskProfileDTOs(newestThreatAssessment));
            model.addAttribute("riskScoreColorMap", scaleService.getScaleRiskScoreColorMap());
            model.addAttribute("unfinishedTasks", taskService.buildRelatedTasks(threatAssessments, true));
        }

		return "assets/view";
	}

    @DeleteMapping("{id}")
    @ResponseStatus(value = HttpStatus.OK)
    @Transactional
    public void assetDelete(@PathVariable final String id) {
        final Long lid = Long.valueOf(id);
        relationService.deleteRelatedTo(lid);
        assetService.deleteById(lid);
    }

	@Transactional
	@PostMapping("dataprocessing")
	public String dataprocessing(@Valid @ModelAttribute final DataProcessingDTO body) {
		final Asset asset = assetService.get(body.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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
        final DataProtectionImpactAssessment dpia = asset.getDpia();
        for (final DataProtectionImpactScreeningAnswerDTO question : dpiaForm.getQuestions()) {
            final DataProtectionImpactScreeningAnswer foundAnswer = dpia.getDpiaScreeningAnswers().stream()
                .filter(a -> a.getChoice().getIdentifier().equalsIgnoreCase(question.getChoice().getIdentifier()))
                .findFirst().orElseGet(() -> {
                    final DataProtectionImpactScreeningAnswer newAnswer = DataProtectionImpactScreeningAnswer.builder()
                        .assessment(dpia)
                        .choice(choiceDPIADao.findByIdentifier(question.getChoice().getIdentifier()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)))
                        .build();
                    dpia.getDpiaScreeningAnswers().add(newAnswer);
                    return newAnswer;
                });
            foundAnswer.setAnswer(question.getAnswer());
        }
        dpia.setAnswerA(dpiaForm.getAnswerA());
        dpia.setAnswerB(dpiaForm.getAnswerB());
        dpia.setAnswerC(dpiaForm.getAnswerC());
        dpia.setAnswerD(dpiaForm.getAnswerD());
        dpia.setConclusion(dpiaForm.getConclusion());
        dpia.setConsequenceLink(dpiaForm.getConsequenceLink());

        return "redirect:/assets/" + dpiaForm.getAssetId();
    }


    @Transactional
    @PostMapping("edit")
    public String formEdit(@ModelAttribute final Asset asset) {
        final Asset existingAsset = assetService.get(asset.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

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

        return "redirect:/assets/" + existingAsset.getId();
    }

	@GetMapping("subsupplier")
	public String subsupplierForm(final Model model, @RequestParam(name = "id", required = false) final Long id,  @RequestParam(name = "asset", required = true) final Long assetId) {
		final ChoiceList acceptanceBasisChoices = choiceService.findChoiceList("dp-supplier-accept-list")
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						"Could not find AcceptanceBasis Choices"));

		model.addAttribute("allSuppliers", supplierDao.findAll());

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
		final Optional<AssetSupplierMapping> subSupplier = asset.getSuppliers().stream().filter(s -> Objects.equals(s.getId(), body.id)).findAny();
		final Supplier supplier = supplierDao.findById(body.supplier).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

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
	@DeleteMapping("subsupplier")
	public String subSupplierDelete(@RequestParam(name = "id") final Long id, @RequestParam(name = "asset") final Long assetId) {
		final Asset asset = assetService.get(assetId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		final AssetSupplierMapping subsupplier = asset.getSuppliers().stream().filter(s -> Objects.equals(s.getId(), id)).findAny()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		asset.getSuppliers().remove(subsupplier);
		return "redirect:/assets/" + asset.getId();
	}


    @Transactional
    @PostMapping("oversight")
    public String oversightSettings(@Valid @ModelAttribute final DataProcessingOversightDTO body) {
        final Asset asset = assetService.get(body.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        asset.setDataProcessingAgreementStatus(body.getDataProcessingAgreementStatus());
        asset.setDataProcessingAgreementLink(body.getDataProcessingAgreementLink());
        asset.setDataProcessingAgreementDate(body.getDataProcessingAgreementDate());
        asset.setSupervisoryModel(body.getSupervisoryModel());
        asset.setNextInspection(body.getNextInspection());
        if (body.getNextInspectionDate() == null) {
            asset.setNextInspectionDate(assetService.getNextInspectionByInterval(asset, LocalDate.now()));
        } else {
            asset.setNextInspectionDate(body.getNextInspectionDate());
        }
        dataProcessingService.createOrUpdateAssociatedOversightCheck(asset);
        return "redirect:/assets/" + asset.getId();
    }


    record AssetOversightDTO (long id, long assetId, User responsibleUser, ChoiceOfSupervisionModel supervisionModel, String conclusion, AssetOversightStatus status, @DateTimeFormat(pattern = "dd/MM-yyyy") LocalDate creationDate, @DateTimeFormat(pattern = "dd/MM-yyyy") LocalDate newInspectionDate, String redirect){
    }
    @Transactional
    @PostMapping("oversight/create")
    public String createOversight(@Valid @ModelAttribute final AssetOversightDTO dto) {
        final Asset asset = assetService.get(dto.assetId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final AssetOversight oversight = new AssetOversight();
        oversight.setAsset(asset);
        oversight.setConclusion(dto.conclusion);
        if (Objects.isNull(dto.creationDate)) {
            oversight.setCreationDate(LocalDate.now());
        } else {
            oversight.setCreationDate(dto.creationDate);
        }
        oversight.setResponsibleUser(dto.responsibleUser);
        oversight.setStatus(dto.status);
        oversight.setSupervisionModel(dto.supervisionModel);

        if (dto.newInspectionDate == null) {
            asset.setNextInspectionDate(assetService.getNextInspectionByInterval(asset, oversight.getCreationDate()));
        } else {
            asset.setNextInspectionDate(dto.newInspectionDate);
        }
        oversight.setNewInspectionDate(asset.getNextInspectionDate());
        final AssetOversight attachedOversight = assetOversightService.create(oversight);
        assetOversightService.createAssociatedCheck(attachedOversight);
        asset.getAssetOversights().add(attachedOversight);
        return dto.redirect.equals("assets") ? "redirect:/assets/" + asset.getId() : "redirect:/suppliers/" + asset.getSupplier().getId();
    }

    @GetMapping("oversight/{id}/{type}")
    public String oversightForm(final Model model, final  @PathVariable("id") Long id, @PathVariable("type") final String type) {
        if(Objects.isNull(id)){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id blev ikke sendt med");
        }
        if(type.equals("asset")) {
            final Asset asset = assetService.get(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Det angivne id for aktiviteten findes ikke")
            );

            model.addAttribute("assetId", asset.getId());
            model.addAttribute("oversight", new AssetOversightDTO(0, 0, new User(), ChoiceOfSupervisionModel.SWORN_STATEMENT, "", AssetOversightStatus.RED, LocalDate.now(), LocalDate.now(), "assets"));
            model.addAttribute("inspectionType", asset.getNextInspection());

            return "assets/fragments/oversightModal";
        }
        if(type.equals("supplier")) {
            final Supplier supplier = supplierDao.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Det angivne id findes ikke"));

            model.addAttribute("oversight", new AssetOversightDTO(0, 0, new User(), ChoiceOfSupervisionModel.SWORN_STATEMENT, "", AssetOversightStatus.RED, LocalDate.now(), LocalDate.now(), "suppliers"));
            model.addAttribute("supplier", supplier);
            model.addAttribute("inspectionType", null);
            model.addAttribute("assetId", null);
            model.addAttribute("supplierAssets", supplier.getAssets());
            return "assets/fragments/oversightModal";
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "typen fandtes ikke: understøttede er 'asset' og 'supplier'");
    }


    @Transactional
    @PostMapping("tia")
    public String tia(@ModelAttribute final Asset asset) {
        final Asset existingAsset = assetService.get(asset.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

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

}

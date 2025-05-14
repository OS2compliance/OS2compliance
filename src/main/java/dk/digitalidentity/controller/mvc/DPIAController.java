package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.Constants;
import dk.digitalidentity.dao.ChoiceDPIADao;
import dk.digitalidentity.model.dto.DataProtectionImpactScreeningAnswerDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.ChoiceDPIA;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.DPIAReport;
import dk.digitalidentity.model.entity.DPIAResponseSection;
import dk.digitalidentity.model.entity.DPIAResponseSectionAnswer;
import dk.digitalidentity.model.entity.DPIATemplateQuestion;
import dk.digitalidentity.model.entity.DPIATemplateSection;
import dk.digitalidentity.model.entity.DataProtectionImpactAssessmentScreening;
import dk.digitalidentity.model.entity.DataProtectionImpactScreeningAnswer;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.RevisionInterval;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentReportApprovalStatus;
import dk.digitalidentity.security.RequireSuperuserOrAdministrator;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.DPIAService;
import dk.digitalidentity.service.DPIATemplateQuestionService;
import dk.digitalidentity.service.DPIATemplateSectionService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.TaskService;
import dk.digitalidentity.service.model.PlaceholderInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("dpia")
@RequireUser
@RequiredArgsConstructor
public class DPIAController {
    private final DPIAService dpiaService;
    private final ChoiceDPIADao choiceDPIADao;
    private final ChoiceService choiceService;
    private final TaskService taskService;
    private final DPIATemplateSectionService dpiaTemplateSectionService;
    private final DPIATemplateQuestionService dpiaTemplateQuestionService;
    private final AssetService assetService;
    private final RelationService relationService;

    @GetMapping
    public String dpiaList(final Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("superuser", authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)));
        return "dpia/index";
    }

	public record DPIAScreeningDTO(List<DataProtectionImpactScreeningAnswerDTO> questions){}
	public record DPIADetailAssetDTO(long id, String name) {}
    public record DPIADetailDTO (long id, String name, List<DPIADetailAssetDTO> assets, String comment) {}
    @GetMapping("{id}")
    public String dpiaDetails(final Model model, @PathVariable Long id) {

        DPIA dpia = dpiaService.find(id);
        List<Asset> assets = dpia.getAssets();

        //
        if (dpia.isFromExternalSource()) {
            return "redirect:" + dpia.getExternalLink();
        }

        // Screening
		if (dpia.getDpiaScreening() == null) {
			DataProtectionImpactAssessmentScreening dataProtectionImpactAssessmentScreening = new DataProtectionImpactAssessmentScreening();
			dataProtectionImpactAssessmentScreening.setConclusion(dpiaService.calculateScreeningConclusion(dataProtectionImpactAssessmentScreening.getDpiaScreeningAnswers()));
			dataProtectionImpactAssessmentScreening.setDpiaScreeningAnswers(new ArrayList<>());
			dpia.setDpiaScreening(dataProtectionImpactAssessmentScreening);
		}
        final List<DataProtectionImpactScreeningAnswerDTO> assetDPIADTOs = new ArrayList<>();
        final List<ChoiceDPIA> choiceDPIA = choiceDPIADao.findAll();
        for (final ChoiceDPIA choice : choiceDPIA) {
            final DataProtectionImpactScreeningAnswer defaultAnswer = new DataProtectionImpactScreeningAnswer();
            defaultAnswer.setAssessment(dpia.getDpiaScreening());
            defaultAnswer.setChoice(choice);
            defaultAnswer.setAnswer(null);
            defaultAnswer.setId(0);
            final DataProtectionImpactScreeningAnswer dpiaAnswer = dpia.getDpiaScreening().getDpiaScreeningAnswers().stream()
                    .filter(m -> Objects.equals(m.getChoice().getId(), choice.getId()))
                    .findAny().orElse(defaultAnswer);
            final DataProtectionImpactScreeningAnswerDTO dpiaDTO = new DataProtectionImpactScreeningAnswerDTO();
            dpiaDTO.setAssetIds(assets.stream().map(Asset::getId).toList());
            dpiaDTO.setAnswer(dpiaAnswer.getAnswer());
            dpiaDTO.setChoice(choice);
            assetDPIADTOs.add(dpiaDTO);
        }

		final DPIAScreeningDTO dpiaScreeningDTO = new DPIAScreeningDTO(assetDPIADTOs);

        //Kvalitetssikring
        final ChoiceList dpiaQualityCheckList = choiceService.findChoiceList("dpia-quality-checklist")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not find dpia quality checklist"));

        //DPIA
        final List<Relatable> allRelatedTo = assets.stream().flatMap(a -> relationService.findAllRelatedTo(a).stream()).toList();
        final List<ThreatAssessment> threatAssessments = allRelatedTo.stream()
				.filter(r -> r.getRelationType() == RelationType.THREAT_ASSESSMENT)
				.map(ThreatAssessment.class::cast)
                .sorted(Comparator.comparing(Relatable::getCreatedAt)
                        .reversed())
                .collect(Collectors.toList());

		model.addAttribute("changeableAsset", assetService.isEditable(assets));
        model.addAttribute("dpia", new DPIADetailDTO(dpia.getId(), dpia.getName(), assets.stream().map(a-> new DPIADetailAssetDTO(a.getId(), a.getName())).toList(), dpia.getComment()));
        model.addAttribute("assets", assets);
		model.addAttribute("assetNames", String.join(", ", dpia.getAssets().stream().map(Asset::getName).toList()));
		model.addAttribute("assetTypeNames", String.join(", ", dpia.getAssets().stream().map(a->a.getAssetType().getCaption()).toList()));
		model.addAttribute("responsibleUserNames", String.join(", ", dpia.getAssets().stream().flatMap(a -> a.getResponsibleUsers().stream().map(User::getName)).toList()));
		model.addAttribute("supplierNames", String.join(", ", dpia.getAssets().stream().map( a -> a.getSupplier().getName()).filter(n -> n != null && n.isBlank()).toList()));
		model.addAttribute("managerNames", String.join(", ", dpia.getAssets().stream().flatMap( a -> a.getManagers().stream().map(User::getName)).toList()));
        model.addAttribute("dpiaScreeningDTO", dpiaScreeningDTO);
		model.addAttribute("qualityassuranceCheckedValues", dpia.getChecks());
        model.addAttribute("dpiaQualityCheckList", dpiaQualityCheckList);
        model.addAttribute("dpiaRevisionTasks", taskService.buildDPIARelatedTasks(dpia.getId(), false));
        model.addAttribute("dpiaSections", buildDPIASections(dpia));
        model.addAttribute("dpiaThreatAssesments", buildDPIAThreatAssessments(dpia, threatAssessments));
        model.addAttribute("dpiaReports", buildDPIAReports(dpia));
        model.addAttribute("conclusion", dpia.getConclusion());

        return "dpia/details";
    }

	public record ExternalDPIAOptionDTO(Long id, String name) {}
	public record ExternalDPIAUserOptionDTO(String uuid, String name) {}
    public record ExternalDPIADTO (Long dpiaId, String externalLink, List<ExternalDPIAOptionDTO> assets, LocalDate userUpdatedDate, String title, ExternalDPIAUserOptionDTO responsibleUser, ExternalDPIAUserOptionDTO responsibleOu ) {}
    @GetMapping("external/{dpiaId}/edit")
    public String editExternalDPIA(final Model model, @PathVariable Long dpiaId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("superuser", authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)));

        DPIA dpia = dpiaService.find(dpiaId);

        model.addAttribute("externalDPIA", new ExternalDPIADTO(
				dpia.getId(),
				dpia.getExternalLink(),
				dpia.getAssets().stream().map(a -> new ExternalDPIAOptionDTO(a.getId(), a.getName())).toList(),
				dpia.getUserUpdatedDate(),
				dpia.getName(),
				new ExternalDPIAUserOptionDTO(dpia.getResponsibleUser().getUuid(), dpia.getResponsibleUser().getName()),
				new ExternalDPIAUserOptionDTO(dpia.getResponsibleOu().getUuid(), dpia.getResponsibleOu().getName())
		));
        return "dpia/fragments/create_external_dpia_modal :: create_external_dpia_modal";
    }

    @GetMapping("external/create")
    public String createExternalDPIA(final Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("superuser", authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)));

        return "dpia/fragments/create_external_dpia_modal :: create_external_dpia_modal";
    }

	public record DPIAResponsibleUser(String uuid, String name){}
	public record DPIAResponsibleOu(String uuid, String name){}
	public record DPIAAssetDTO(long id, String name){}
	@RequireSuperuserOrAdministrator
	@GetMapping("{dpiaId}/edit")
	public String getEditFragment(final Model model, @PathVariable Long dpiaId) {
		final DPIA dpia = dpiaService.find(dpiaId);
		model.addAttribute("title", dpia.getName());
		model.addAttribute("userUpdatedDate", dpia.getUserUpdatedDate());
		model.addAttribute("dpiaId", dpia.getId());
		model.addAttribute("assets", dpia.getAssets().stream().map(a -> new DPIAAssetDTO(a.getId(), a.getName())).toList());
		model.addAttribute("responsibleUser", new DPIAResponsibleUser(dpia.getResponsibleUser().getUuid(), dpia.getResponsibleUser().getName()));
		model.addAttribute("responsibleOu", new DPIAResponsibleOu(dpia.getResponsibleOu().getUuid(), dpia.getResponsibleOu().getName()));
		return "dpia/fragments/edit_dpia_modal :: edit_dpia_modal";
	}


    record DPIAQuestionDTO(long id, long questionResponseId, String question, String instructions, String templateAnswer, String response) {}
    record DPIASectionDTO(long id, String sectionIdentifier, long sectionResponseId, String heading, String explainer, boolean canOptOut, boolean hasOptedOutResponse, List<DPIAQuestionDTO> questions) {}
    private List<DPIASectionDTO> buildDPIASections(DPIA dpia) {
        List<DPIASectionDTO> sections = new ArrayList<>();
        List<DPIATemplateSection> allSections = dpiaTemplateSectionService.findAll().stream()
            .sorted(Comparator.comparing(DPIATemplateSection::getSortKey))
            .toList();

//        // needed dataprocessing fields
//        PlaceholderInfo placeholderInfo = assetService.getDPIAResponsePlaceholderInfo(dpia.getAssets());

        for (DPIATemplateSection templateSection : allSections) {
            if (templateSection.isHasOptedOut()) {
                continue;
            }

            List<DPIAQuestionDTO> questionDTOS = new ArrayList<>();
            DPIAResponseSection matchSection = dpia.getDpiaResponseSections().stream().filter(s -> s.getDpiaTemplateSection().getId() == templateSection.getId()).findAny().orElse(null);
            List<DPIATemplateQuestion> questions = templateSection.getDpiaTemplateQuestions().stream()
                .sorted(Comparator.comparing(DPIATemplateQuestion::getSortKey))
                .toList();
            for (DPIATemplateQuestion templateQuestion : questions) {
                DPIAResponseSectionAnswer matchAnswer = matchSection == null ? null : matchSection.getDpiaResponseSectionAnswers().stream().filter(s -> s.getDpiaTemplateQuestion().getId() == templateQuestion.getId()).findAny().orElse(null);

                String templateAnswer = templateQuestion.getAnswerTemplate() == null ? "" : templateQuestion.getAnswerTemplate();

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
    private List<DPIAThreatAssessmentDTO> buildDPIAThreatAssessments(DPIA dpia, List<ThreatAssessment> threatAssessments) {
        List<DPIAThreatAssessmentDTO> result = new ArrayList<>();
        Set<String> selectedThreatAssessments = dpia.getCheckedThreatAssessmentIds() == null ? new HashSet<>() : Arrays.stream(dpia.getCheckedThreatAssessmentIds().split(",")).collect(Collectors.toSet());
        for (ThreatAssessment threatAssessment : threatAssessments) {
            boolean selected = selectedThreatAssessments.contains(threatAssessment.getId().toString());
            String date = threatAssessment.getCreatedAt().format(Constants.DK_DATE_FORMATTER);
            DPIAThreatAssessmentDTO dto = new DPIAThreatAssessmentDTO(selected, threatAssessment.getId(), threatAssessment.getName(), date, threatAssessment.getThreatAssessmentReportApprovalStatus().equals(ThreatAssessmentReportApprovalStatus.SIGNED));
            result.add(dto);
        }
        return result;
    }

    record DPIAReportDTO(long s3DocumentId, String approverName, String status, String date) {}
    private List<DPIAReportDTO> buildDPIAReports(DPIA dpia) {
        List<DPIAReportDTO> result = new ArrayList<>();
        final List<DPIAReport> sortedDpiaReports = dpia.getDpiaReports();
        sortedDpiaReports.sort(Comparator.comparing(s -> s.getDpiaReportS3Document().getTimestamp()));
        for (DPIAReport sortedDpiaReport : sortedDpiaReports) {
            String date = sortedDpiaReport.getDpiaReportS3Document().getTimestamp().format(Constants.DK_DATE_FORMATTER);
            result.add(new DPIAReportDTO(sortedDpiaReport.getDpiaReportS3Document().getId(), sortedDpiaReport.getReportApproverName(), sortedDpiaReport.getDpiaReportApprovalStatus().getMessage(), date));
        }
        return result;
    }

    public record RevisionFormDTO(@DateTimeFormat(pattern = "dd/MM-yyyy") LocalDate nextRevision, RevisionInterval revisionInterval) {}
    @GetMapping("{dpiaId}/revision")
    public String revisionForm(final Model model, @PathVariable final long dpiaId) {
         DPIA dpia = dpiaService.find(dpiaId);
        assetService.updateNextRevisionAssociatedTask(dpia);
        model.addAttribute("dpiaId", dpia.getId());
        model.addAttribute("RevisionFormDTO", new RevisionFormDTO(dpia.getNextRevision(), dpia.getRevisionInterval()));
        return "dpia/fragments/revisionIntervalForm";
    }

    @PostMapping("{dpiaId}/revision")
    @Transactional
    public String postRevisionForm(@ModelAttribute final RevisionFormDTO revisionFormDTO, @PathVariable final long dpiaId) {
        DPIA dpia = dpiaService.find(dpiaId);
        final List<Asset> assets = dpia.getAssets();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !isResponsibleForAsset(assets)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        dpia.setRevisionInterval(revisionFormDTO.revisionInterval);
        dpia.setNextRevision(revisionFormDTO.nextRevision);
        assetService.createOrUpdateAssociatedCheck(dpia);
        return "redirect:/dpia/" + dpia.getId();
    }

	private boolean isResponsibleForAsset(List<Asset> assets) {
		return assets.stream().flatMap(a ->
						a.getResponsibleUsers().stream()
								.map(User::getUuid))
				.toList()
				.contains(SecurityUtil.getPrincipalUuid());
	}
}

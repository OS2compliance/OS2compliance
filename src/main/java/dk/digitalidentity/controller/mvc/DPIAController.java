package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.Constants;
import dk.digitalidentity.dao.ChoiceDPIADao;
import dk.digitalidentity.model.dto.DataProtectionImpactDTO;
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
import dk.digitalidentity.model.entity.DataProtectionImpactScreeningAnswer;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.enums.DPIAAnswerPlaceholder;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentReportApprovalStatus;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

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

    public record DPIADetailDTO (long id, long assetId, String assetName) {}
    @GetMapping("{id}")
    public String dpiaList(final Model model, @PathVariable Long id) {
        DPIA dpia = dpiaService.find(id);
        Asset asset = dpia.getAsset();

        // Screening
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
                .comment(asset.getDpia() == null ? "" : asset.getDpia().getComment())
                .build();

        //Kvalitetssikring
        final ChoiceList dpiaQualityCheckList = choiceService.findChoiceList("dpia-quality-checklist")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not find dpia quality checklist"));

        //DPIA
        final List<Relatable> allRelatedTo = relationService.findAllRelatedTo(asset);
        final List<ThreatAssessment> threatAssessments = allRelatedTo.stream()
            .filter(r -> r.getRelationType() == RelationType.THREAT_ASSESSMENT)
            .map(ThreatAssessment.class::cast)
            .collect(Collectors.toList());
        threatAssessments.sort(Comparator.comparing(Relatable::getCreatedAt).reversed());


        model.addAttribute("dpia", new DPIADetailDTO(dpia.getId(), asset.getId(), asset.getName()));
        model.addAttribute("asset", asset);
        model.addAttribute("dpiaForm", dpiaForm);
        model.addAttribute("dpiaQualityCheckList", dpiaQualityCheckList);
        model.addAttribute("dpiaRevisionTasks", taskService.buildDPIARelatedTasks(asset, false));
        model.addAttribute("dpiaSections", buildDPIASections(asset));
        model.addAttribute("dpiaThreatAssesments", buildDPIAThreatAssessments(asset, threatAssessments));
        model.addAttribute("dpiaReports", buildDPIAReports(asset));
        model.addAttribute("conclusion", asset.getDpia().getConclusion());

        return "dpia/details";
    }

    private List<AssetsController.DPIASectionDTO> buildDPIASections(Asset asset) {
        List<AssetsController.DPIASectionDTO> sections = new ArrayList<>();
        List<DPIATemplateSection> allSections = dpiaTemplateSectionService.findAll().stream()
            .sorted(Comparator.comparing(DPIATemplateSection::getSortKey))
            .collect(Collectors.toList());

        // needed dataprocessing fields
        PlaceholderInfo placeholderInfo = assetService.getDPIAResponsePlaceholderInfo(asset);

        for (DPIATemplateSection templateSection : allSections) {
            if (templateSection.isHasOptedOut()) {
                continue;
            }

            List<AssetsController.DPIAQuestionDTO> questionDTOS = new ArrayList<>();
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
                    questionDTOS.add(new AssetsController.DPIAQuestionDTO(templateQuestion.getId(), 0, templateQuestion.getQuestion(), templateQuestion.getInstructions(), templateAnswer, ""));
                } else {
                    questionDTOS.add(new AssetsController.DPIAQuestionDTO(templateQuestion.getId(), matchAnswer.getId(), templateQuestion.getQuestion(), templateQuestion.getInstructions(), templateAnswer, matchAnswer.getResponse()));
                }
            }

            if (matchSection == null) {
                sections.add(new AssetsController.DPIASectionDTO(templateSection.getId(), templateSection.getIdentifier(), 0, templateSection.getHeading(), templateSection.getExplainer(), templateSection.isCanOptOut(), false, questionDTOS));
            } else {
                sections.add(new AssetsController.DPIASectionDTO(templateSection.getId(), templateSection.getIdentifier(), matchSection.getId(), templateSection.getHeading(), templateSection.getExplainer(), templateSection.isCanOptOut(), !matchSection.isSelected(), questionDTOS));
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
}

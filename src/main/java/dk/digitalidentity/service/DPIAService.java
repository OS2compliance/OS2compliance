package dk.digitalidentity.service;

import dk.digitalidentity.dao.DPIADao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.DPIAResponseSection;
import dk.digitalidentity.model.entity.DPIAResponseSectionAnswer;
import dk.digitalidentity.model.entity.DPIATemplateQuestion;
import dk.digitalidentity.model.entity.DataProtectionImpactAssessmentScreening;
import dk.digitalidentity.model.entity.DataProtectionImpactScreeningAnswer;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.DPIAAnswerPlaceholder;
import dk.digitalidentity.model.entity.enums.DPIAScreeningConclusion;
import dk.digitalidentity.service.model.PlaceholderInfo;
import lombok.RequiredArgsConstructor;
import org.htmlcleaner.BrowserCompactXmlSerializer;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DPIAService {
    private final DPIADao dpiaDao;
    private final DPIATemplateSectionService dpiaTemplateSectionService;
    private final AssetService assetService;
    private final DPIATemplateQuestionService dpiaTemplateQuestionService;
    private final DPIAResponseSectionService dpiaResponseSectionService;
    private final DPIAResponseSectionAnswerService dpiaResponseSectionAnswerService;
	private final UserService userService;
	private final OrganisationService organisationService;

    public DPIA find (Long dpiaId) {
        return dpiaDao.findById(dpiaId)
            .orElseThrow();
    }

	public List<DPIA> findAll() {
		return dpiaDao.findAll();
	}

	@Transactional
    public DPIA save(DPIA dpia) {
        return dpiaDao.save(dpia);
    }

    @Transactional
    public void delete(Long dpiaId) {
        DPIA dpia = dpiaDao.findById(dpiaId)
            .orElseThrow();

        dpiaDao.deleteById(dpiaId);
    }

    /**
     * Creates a new DPIA, saving it to the db and returning the saved entity
     * @param assets
     * @param name
     * @return
     */
	@Transactional
    public DPIA create(List<Asset> assets, String name, LocalDate userUpdatedDate, String responsibleUserUuid, String responsibleOuUuid) throws IOException {
        // default dpia creation
        DPIA dpia = new DPIA();

		if (name == null || name.isEmpty()) {
			dpia.setName(assets.size()>1 ? "Konsekvensanalyse for" + assets.getFirst().getName() + " med flere" : "Konsekvensanalyse for" + assets.getFirst().getName());
		} else {
        	dpia.setName(name);
		}
        dpia.setAssets(assets);
		dpia.setUserUpdatedDate(userUpdatedDate);

		if (responsibleUserUuid != null) {
			User user = userService.findByUuid(responsibleUserUuid).orElse(null);
			dpia.setResponsibleUser(user);
		}
		if (responsibleOuUuid != null) {
			OrganisationUnit ou = organisationService.get(responsibleOuUuid).orElse(null);
			dpia.setResponsibleOu(ou);
		}

		DataProtectionImpactAssessmentScreening screening = new DataProtectionImpactAssessmentScreening();
		screening.setConclusion(calculateScreeningConclusion(screening.getDpiaScreeningAnswers()));
		dpia.setDpiaScreening(screening);
		screening.setDpia(dpia);

        dpia = save(dpia);

        //find all templated answers
        List<DPIATemplateQuestion> dpiaTemplateQuestions = dpiaTemplateQuestionService.findByAnswerTemplateNotNull();
        List<PlaceholderInfo> placeholderInfo = assets.stream().map(assetService::getDPIAResponsePlaceholderInfo).toList();
        for (DPIATemplateQuestion templateQuestion : dpiaTemplateQuestions) {
            String templateAnswer = templateQuestion.getAnswerTemplate()
                .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_DATA_WHO.getPlaceholder(), String.join(", ", placeholderInfo.stream()
						.map(PlaceholderInfo::getSelectedAccessWhoTitles)
						.filter(s->!s.isEmpty())
						.toList()))
                .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_DATA_HOW_MANY.getPlaceholder(), String.join(", ", placeholderInfo.stream()
						.map(PlaceholderInfo::getSelectedAccessCountTitle)
						.filter(s->!s.isEmpty())
						.toList()))
                .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_DATA_TYPES.getPlaceholder(), String.join(", ", placeholderInfo.stream()
						.flatMap(p -> p.getPersonalDataTypesTitles().stream())
						.filter(s->!s.isEmpty())
						.toList()))
                .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_DATA_TYPES_FREETEXT.getPlaceholder(), String.join(", ", placeholderInfo.stream()
						.map(PlaceholderInfo::getTypesOfPersonalInformationFreetext)
						.filter(s->!s.isEmpty())
						.toList()))
                .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_CATEGORIES_OF_REGISTERED.getPlaceholder(), String.join(", ", placeholderInfo.stream()
						.flatMap(p -> p.getCategoriesOfRegisteredTitles().stream())
						.filter(s->!s.isEmpty())
						.toList()))
                .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_HOW_LONG.getPlaceholder(), String.join(", ", placeholderInfo.stream()
						.map(PlaceholderInfo::getHowLongTitle)
						.filter(s->!s.isEmpty())
						.toList()))
                .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_DELETE_LINK.getPlaceholder(), String.join(", ", assets.stream()
						.filter(a-> a.getDataProcessing().getDeletionProcedureLink() != null && !a.getDataProcessing().getDeletionProcedureLink().isBlank())
						.map(a -> "<a href=\"" + a.getDataProcessing().getDeletionProcedureLink() + "\">" + a.getDataProcessing().getDeletionProcedureLink() + "</a>")
						.toList()));


            //create new DPIAResponseSection & DPIAResponseSectionAnswer with the templated answers
            DPIAResponseSection responseSection = new DPIAResponseSection();
            responseSection.setDpia(dpia);
            responseSection.setDpiaTemplateSection(templateQuestion.getDpiaTemplateSection());
            responseSection.setSelected(true);
            responseSection = dpiaResponseSectionService.save(responseSection);
            dpia.getDpiaResponseSections().add(responseSection);

            DPIAResponseSectionAnswer answer = new DPIAResponseSectionAnswer();
            answer.setDpiaResponseSection(responseSection);
            answer.setDpiaTemplateQuestion(templateQuestion);
            String xhtml = toXHTML(templateAnswer);
            answer.setResponse(xhtml);
            answer = dpiaResponseSectionAnswerService.save(answer);
            responseSection.getDpiaResponseSectionAnswers().add(answer);

        }

        return save(dpia);
    }

    /**
     * Creates a default external DPIA, saving it to db and returning the saved entity
     * @param assets
     * @param externalLink
     * @param name Name of the DPIA. If null, set to default (asset.name + " Konsekvensaanalyse")
     * @return
     */
	@Transactional
    public DPIA createExternal(List<Asset> assets, String externalLink, String name, LocalDate userUpdatedDate,String responsibleUserUuid, String responsibleOuUuid) {
        DPIA dpia = new DPIA();
        dpia.setName(name);
        dpia.setAssets(assets);
        dpia.setFromExternalSource(true);
        dpia.setUserUpdatedDate(userUpdatedDate);
        dpia.setExternalLink(externalLink);
		if (responsibleUserUuid != null) {
			User user = userService.findByUuid(responsibleUserUuid).orElse(null);
			dpia.setResponsibleUser(user);
		}
		if (responsibleOuUuid != null) {
			OrganisationUnit ou = organisationService.get(responsibleOuUuid).orElse(null);
			dpia.setResponsibleOu(ou);
		}
        return save(dpia);
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

        return (bos.toString(StandardCharsets.UTF_8));
    }

	/**
	 * Calculates the danger indicator for a screening by the following rules:
	 * If all answers are blank -> Grey
	 * If one of the "critical" questions have a dangerous answer -> Red
	 * If two or more sections contains dangerous answers -> Red
	 * If exactly one section contains dangerous answers - > Yellow
	 * Otherwise -> Green
	 * @param screeningAnswers
	 * @return
	 */
	public DPIAScreeningConclusion calculateScreeningConclusion( List<DataProtectionImpactScreeningAnswer> screeningAnswers) {
		if(screeningAnswers.stream().allMatch(a -> a.getAnswer().isBlank())) {
			return DPIAScreeningConclusion.GREY;
		}

		List<String> dangerousAnswers = List.of("dpia-yes", "dpia-dont-know", "dpia-partially");
		List<String> criticalQuestionsIdentifiers = List.of("dpia-7");

		boolean containsCritical = screeningAnswers.stream()
				.filter(s -> criticalQuestionsIdentifiers.contains(s.getChoice().getIdentifier()))
				.anyMatch(s -> dangerousAnswers.contains(s.getAnswer()));
		if (containsCritical) {
			return DPIAScreeningConclusion.RED;
		}

		Set<String> dangerousSections = new HashSet<>();

		for (var answer : screeningAnswers) {
			if (dangerousAnswers.contains(answer.getAnswer())){
				dangerousSections.add(answer.getChoice().getCategory());
			}
		}

		int dangerousAnswerCount = dangerousSections.size();

		if(dangerousAnswerCount == 1) {
			return DPIAScreeningConclusion.YELLOW;
		} else if(dangerousAnswerCount > 1) {
			return DPIAScreeningConclusion.RED;
		}
		return DPIAScreeningConclusion.GREEN;
	}

	public Set<DPIA> findByOwnedAsset(String userUuid) {
		return dpiaDao.findByAssets_ResponsibleUsers_UuidContainsOrAssets_Managers_UuidContains(userUuid, userUuid);
	}
}

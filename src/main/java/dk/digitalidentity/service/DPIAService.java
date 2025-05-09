package dk.digitalidentity.service;

import dk.digitalidentity.dao.DPIADao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.DPIAResponseSection;
import dk.digitalidentity.model.entity.DPIAResponseSectionAnswer;
import dk.digitalidentity.model.entity.DPIATemplateQuestion;
import dk.digitalidentity.model.entity.enums.DPIAAnswerPlaceholder;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class DPIAService {
    private final DPIADao dpiaDao;
    private final DPIATemplateSectionService dpiaTemplateSectionService;
    private final AssetService assetService;
    private final DPIATemplateQuestionService dpiaTemplateQuestionService;
    private final DPIAResponseSectionService dpiaResponseSectionService;
    private final DPIAResponseSectionAnswerService dpiaResponseSectionAnswerService;

    public DPIA find (Long dpiaId) {
        return dpiaDao.findById(dpiaId)
            .orElseThrow();
    }

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
     * @param asset
     * @param name Name of the DPIA. If null, set to default (asset.name + " Konsekvensaanalyse")
     * @return
     */
    public DPIA create(Asset asset, String name, LocalDate userUpdatedDate) throws IOException {
        // default dpia creation
        DPIA dpia = new DPIA();
        dpia.setName(name != null ? name : asset.getName()+" Konsekvensaanalyse");
        dpia.setAsset(asset);
		dpia.setUserUpdatedDate(userUpdatedDate);
        dpia = save(dpia);

        //find all templated answers
        List<DPIATemplateQuestion> dpiaTemplateQuestions = dpiaTemplateQuestionService.findByAnswerTemplateNotNull();
        PlaceholderInfo placeholderInfo = assetService.getDPIAResponsePlaceholderInfo(asset);
        for (DPIATemplateQuestion templateQuestion : dpiaTemplateQuestions) {
            String templateAnswer = templateQuestion.getAnswerTemplate()
                .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_DATA_WHO.getPlaceholder(), placeholderInfo.getSelectedAccessWhoTitles())
                .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_DATA_HOW_MANY.getPlaceholder(), placeholderInfo.getSelectedAccessCountTitle())
                .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_DATA_TYPES.getPlaceholder(), String.join(", ", placeholderInfo.getPersonalDataTypesTitles()))
                .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_DATA_TYPES_FREETEXT.getPlaceholder(), placeholderInfo.getTypesOfPersonalInformationFreetext())
                .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_CATEGORIES_OF_REGISTERED.getPlaceholder(), String.join(", ", placeholderInfo.getCategoriesOfRegisteredTitles()))
                .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_HOW_LONG.getPlaceholder(), placeholderInfo.getHowLongTitle())
                .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_DELETE_LINK.getPlaceholder(), dpia.getAsset().getDataProcessing().getDeletionProcedureLink() == null ? "" : "<a href=\"" + dpia.getAsset().getDataProcessing().getDeletionProcedureLink() + "\">" + dpia.getAsset().getDataProcessing().getDeletionProcedureLink() + "</a>");


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
     * @param asset
     * @param externalLink
     * @param name Name of the DPIA. If null, set to default (asset.name + " Konsekvensaanalyse")
     * @return
     */
    public DPIA createExternal(Asset asset, String externalLink, String name, LocalDate userUpdatedDate) {
        DPIA dpia = new DPIA();
        dpia.setName(name != null ? name : asset.getName()+" Konsekvensaanalyse");
        dpia.setAsset(asset);
        dpia.setFromExternalSource(true);
        dpia.setUserUpdatedDate(userUpdatedDate);
        dpia.setExternalLink(externalLink);
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
}

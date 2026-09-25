package dk.digitalidentity.service;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.Constants;
import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.dao.ChoiceListDao;
import dk.digitalidentity.dao.ChoiceValueDao;
import dk.digitalidentity.dao.DPIATemplateQuestionDao;
import dk.digitalidentity.dao.DPIATemplateSectionDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.DPIAResponseSectionAnswer;
import dk.digitalidentity.model.entity.DPIATemplateQuestion;
import dk.digitalidentity.model.entity.DPIATemplateSection;
import dk.digitalidentity.model.entity.DataProcessing;
import dk.digitalidentity.model.entity.enums.AssetStatus;
import dk.digitalidentity.model.entity.enums.ContainsAITechnologyEnum;
import dk.digitalidentity.model.entity.enums.Criticality;
import dk.digitalidentity.model.entity.enums.DataProcessingAgreementStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A question removed from the DPIA template is only soft deleted (deleted = true). It must not
 * resurface in konsekvensanalyser created afterwards, e.g. as a pre-filled templated answer.
 */
@Transactional
public class DPIATemplateQuestionSoftDeleteTest extends BaseIntegrationTest {

    @Autowired private DPIAService dpiaService;
    @Autowired private DPIATemplateSectionDao dpiaTemplateSectionDao;
    @Autowired private DPIATemplateQuestionDao dpiaTemplateQuestionDao;
    @Autowired private AssetDao assetDao;
    @Autowired private ChoiceValueDao choiceValueDao;
    @Autowired private ChoiceListDao choiceListDao;

    @Test
    public void deletedTemplateQuestionIsNotIncludedInNewDPIA() throws IOException {
        ensureDataProcessingChoiceLists();

        // Questions carrying an answerTemplate are the ones pre-filled on DPIA creation.
        final DPIATemplateSection section = aTemplateSectionWithTwoTemplatedQuestions();
        final DPIATemplateQuestion kept = section.getDpiaTemplateQuestions().getFirst();
        final DPIATemplateQuestion removed = section.getDpiaTemplateQuestions().getLast();

        removed.setDeleted(true);
        dpiaTemplateQuestionDao.save(removed);

        assertThat(dpiaTemplateQuestionDao.findByAnswerTemplateNotNullAndDeletedFalse())
            .extracting(DPIATemplateQuestion::getId)
            .contains(kept.getId())
            .doesNotContain(removed.getId());

        final DPIA dpia = dpiaService.create(new ArrayList<>(List.of(anAsset())), "Konsekvensanalyse for test", LocalDate.now(), null, null);

        final List<Long> answeredQuestionIds = dpia.getDpiaResponseSections().stream()
            .flatMap(s -> s.getDpiaResponseSectionAnswers().stream())
            .map(DPIAResponseSectionAnswer::getDpiaTemplateQuestion)
            .map(DPIATemplateQuestion::getId)
            .toList();
        assertThat(answeredQuestionIds)
            .contains(kept.getId())
            .doesNotContain(removed.getId());
    }

    /**
     * The placeholders in a templated answer are resolved from the data processing choice lists, which
     * are seeded from json in production but not in the test database.
     */
    private void ensureDataProcessingChoiceLists() {
        List.of("dp-access-who-list", "dp-access-count-list", "dp-person-storage-duration-list",
                "dp-person-categories-list", "dp-person-categories-sensitive-list", "dp-categories-list")
            .forEach(identifier -> {
                if (choiceListDao.findByIdentifier(identifier).isEmpty()) {
                    choiceListDao.save(ChoiceList.builder().identifier(identifier).name(identifier).multiSelect(false).build());
                }
            });
    }

    private DPIATemplateSection aTemplateSectionWithTwoTemplatedQuestions() {
        final DPIATemplateSection section = new DPIATemplateSection();
        section.setIdentifier("test_legal_ai");
        section.setHeading("Vurdering af lovlighed ved AI");
        section.setSortKey(1L);
        section.getDpiaTemplateQuestions().add(templatedQuestion(section, 0L, "Princippet om lovlighed"));
        section.getDpiaTemplateQuestions().add(templatedQuestion(section, 1L, "Princippet om lovlighed (dublet)"));
        return dpiaTemplateSectionDao.save(section);
    }

    private DPIATemplateQuestion templatedQuestion(final DPIATemplateSection section, final long sortKey, final String question) {
        final DPIATemplateQuestion templateQuestion = new DPIATemplateQuestion();
        templateQuestion.setDpiaTemplateSection(section);
        templateQuestion.setSortKey(sortKey);
        templateQuestion.setQuestion(question);
        templateQuestion.setInstructions("Beskriv vurderingen");
        templateQuestion.setAnswerTemplate("<p>Svarskabelon</p>");
        return templateQuestion;
    }

    private Asset anAsset() {
        final ChoiceValue assetType = choiceValueDao.findByIdentifier(Constants.CHOICE_LIST_ASSET_IT_SYSTEM_TYPE_ID)
            .orElseThrow(() -> new IllegalStateException("IT system asset type not seeded"));

        final Asset asset = new Asset();
        asset.setName("Foo");
        asset.setAssetType(assetType);
        asset.setAssetStatus(AssetStatus.NOT_STARTED);
        asset.setAiStatus(ContainsAITechnologyEnum.UNDECIDED);
        asset.setCriticality(Criticality.CRITICAL);
        asset.setDataProcessingAgreementStatus(DataProcessingAgreementStatus.NOT_RELEVANT);
        asset.setDataProcessing(new DataProcessing());
        return assetDao.save(asset);
    }
}

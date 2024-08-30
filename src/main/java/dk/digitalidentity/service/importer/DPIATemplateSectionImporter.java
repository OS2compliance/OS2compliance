package dk.digitalidentity.service.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.digitalidentity.dao.DPIATemplateSectionDao;
import dk.digitalidentity.model.dto.DPIATemplateQuestionDTO;
import dk.digitalidentity.model.dto.DPIATemplateSectionDTO;
import dk.digitalidentity.model.entity.DPIATemplateQuestion;
import dk.digitalidentity.model.entity.DPIATemplateSection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DPIATemplateSectionImporter {
    private final ObjectMapper objectMapper;
    private final DPIATemplateSectionDao dpiaTemplateSectionDao;

    public DPIATemplateSectionImporter(final ObjectMapper objectMapper, final DPIATemplateSectionDao dpiaTemplateSectionDao) {
        this.objectMapper = objectMapper;
        this.dpiaTemplateSectionDao = dpiaTemplateSectionDao;
    }

    public void importDPIATemplateSections(final String filename) throws IOException {
        final DPIATemplateSectionDTO[] values = objectMapper.readValue(new ClassPathResource(filename).getInputStream(), DPIATemplateSectionDTO[].class);

        List<DPIATemplateSection> dpiaTemplateSections = new ArrayList<>();
        for (DPIATemplateSectionDTO dto : values) {
            DPIATemplateSection dpiaTemplateSection = new DPIATemplateSection();
            dpiaTemplateSection.setSortKey(dto.getSortKey());
            dpiaTemplateSection.setHeading(dto.getHeader());
            dpiaTemplateSection.setExplainer(dto.getExplainer());
            dpiaTemplateSection.setCanOptOut(dto.isCanOptOut());
            dpiaTemplateSection.setDpiaTemplateQuestions(new ArrayList<>());

            if (dto.getDpiaTemplateQuestions() != null) {
                for (DPIATemplateQuestionDTO dpiaTemplateQuestionDTO : dto.getDpiaTemplateQuestions()) {
                    DPIATemplateQuestion question = new DPIATemplateQuestion();
                    question.setDpiaTemplateSection(dpiaTemplateSection);
                    question.setSortKey(dpiaTemplateQuestionDTO.getSortKey());
                    question.setQuestion(dpiaTemplateQuestionDTO.getQuestion());
                    question.setInstructions(dpiaTemplateQuestionDTO.getInstructions());
                    question.setAnswerTemplate(dpiaTemplateQuestionDTO.getAnswerTemplate());
                    dpiaTemplateSection.getDpiaTemplateQuestions().add(question);
                }
            }

            dpiaTemplateSections.add(dpiaTemplateSection);
        }

        dpiaTemplateSectionDao.saveAll(dpiaTemplateSections);
    }
}

package dk.digitalidentity.service;

import dk.digitalidentity.dao.DPIATemplateQuestionDao;
import dk.digitalidentity.model.entity.DPIATemplateQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DPIATemplateQuestionService {

    private final DPIATemplateQuestionDao dpiaTemplateQuestionDao;

    public Optional<DPIATemplateQuestion> findById(long id) {
        return dpiaTemplateQuestionDao.findById(id);
    }

    public void save(DPIATemplateQuestion dpiaTemplateQuestion) {
        dpiaTemplateQuestionDao.save(dpiaTemplateQuestion);
    }

    public List<DPIATemplateQuestion> findAll() {
        return dpiaTemplateQuestionDao.findAll();
    }
}

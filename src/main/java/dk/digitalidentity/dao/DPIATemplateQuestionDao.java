package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.DPIATemplateQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DPIATemplateQuestionDao extends JpaRepository<DPIATemplateQuestion, Long> {

    List<DPIATemplateQuestion> findByAnswerTemplateNotNull();
}

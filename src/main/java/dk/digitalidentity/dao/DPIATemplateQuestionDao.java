package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.DPIATemplateQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DPIATemplateQuestionDao extends JpaRepository<DPIATemplateQuestion, Long> {
}

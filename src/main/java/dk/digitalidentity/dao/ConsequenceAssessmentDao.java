package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.ConsequenceAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsequenceAssessmentDao extends JpaRepository<ConsequenceAssessment, Long> {
}

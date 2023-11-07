package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.ThreatAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThreatAssessmentDao extends JpaRepository<ThreatAssessment, Long> {
}

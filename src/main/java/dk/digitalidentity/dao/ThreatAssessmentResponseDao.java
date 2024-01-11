package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThreatAssessmentResponseDao extends JpaRepository<ThreatAssessmentResponse, Long> {
}

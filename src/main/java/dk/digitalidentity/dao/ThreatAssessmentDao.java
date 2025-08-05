package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThreatAssessmentDao extends JpaRepository<ThreatAssessment, Long> {

    long countByThreatCatalog(final ThreatCatalog threatCatalog);
    ThreatAssessment findByThreatAssessmentReportS3DocumentId(long id);
	List<ThreatAssessment> findByDeletedFalseAndThreatAssessmentTypeIn(List<ThreatAssessmentType> types);
	List<ThreatAssessment> findAllByDeletedFalse();
}

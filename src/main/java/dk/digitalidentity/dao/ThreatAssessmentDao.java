package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThreatAssessmentDao extends JpaRepository<ThreatAssessment, Long> {

    long countByThreatCatalog(final ThreatCatalog threatCatalog);
    ThreatAssessment findByThreatAssessmentReportS3DocumentId(long id);

}

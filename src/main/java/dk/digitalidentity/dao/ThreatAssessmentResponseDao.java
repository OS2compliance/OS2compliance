package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface ThreatAssessmentResponseDao extends JpaRepository<ThreatAssessmentResponse, Long> {

    long countByThreatCatalogThreat(final ThreatCatalogThreat threat);

	@Modifying
	@Query("DELETE FROM ThreatAssessmentResponse r WHERE r.threatAssessment.id = :assessmentId AND r.threatCatalogThreat.threatCatalog.identifier IN :catalogIdentifiers")
	void deleteResponsesByAssessmentAndCatalogIdentifiers(@Param("assessmentId") Long assessmentId, @Param("catalogIdentifiers") Set<String> catalogIdentifiers);

}

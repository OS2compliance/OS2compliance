package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Set;

public interface ThreatAssessmentDao extends JpaRepository<ThreatAssessment, Long> {

    long countByThreatCatalog(final ThreatCatalog threatCatalog);
    ThreatAssessment findByThreatAssessmentReportS3DocumentId(long id);

	Set<ThreatAssessment> findByThreatAssessmentTypeIn(Collection<ThreatAssessmentType> threatAssessmentTypes);

	@Query("""
			SELECT ta FROM Asset a, Relation r, ThreatAssessment ta
			WHERE (
			    (r.relationAId = a.id AND r.relationAType = 'ASSET' AND
			     r.relationBType = 'THREAT_ASSESSMENT' AND ta.id = r.relationBId)
			    OR
			    (r.relationBId = a.id AND r.relationBType = 'ASSET' AND
			     r.relationAType = 'THREAT_ASSESSMENT' AND ta.id = r.relationAId)
			)
			AND ta.updatedAt = (
			    SELECT MAX(ta2.updatedAt)
			    FROM Relation r2, ThreatAssessment ta2
			    WHERE (
			        (r2.relationAId = a.id AND r2.relationAType = 'ASSET' AND
			         r2.relationBType = 'THREAT_ASSESSMENT' AND ta2.id = r2.relationBId)
			        OR
			        (r2.relationBId = a.id AND r2.relationBType = 'ASSET' AND
			         r2.relationAType = 'THREAT_ASSESSMENT' AND ta2.id = r2.relationAId)
			    )
			)
			""")
	Set<ThreatAssessment> findLatestForAllAssets();

	@Query("""
			SELECT ta FROM Register ŕ, Relation r, ThreatAssessment ta
			WHERE (
			    (r.relationAId = r.id AND r.relationAType = 'REGISTER' AND
			     r.relationBType = 'THREAT_ASSESSMENT' AND ta.id = r.relationBId)
			    OR
			    (r.relationBId = r.id AND r.relationBType = 'REGISTER' AND
			     r.relationAType = 'THREAT_ASSESSMENT' AND ta.id = r.relationAId)
			)
			AND ta.updatedAt = (
			    SELECT MAX(ta2.updatedAt)
			    FROM Relation r2, ThreatAssessment ta2
			    WHERE (
			        (r2.relationAId = r.id AND r2.relationAType = 'REGISTER' AND
			         r2.relationBType = 'THREAT_ASSESSMENT' AND ta2.id = r2.relationBId)
			        OR
			        (r2.relationBId = r.id AND r2.relationBType = 'REGISTER' AND
			         r2.relationAType = 'THREAT_ASSESSMENT' AND ta2.id = r2.relationAId)
			    )
			)
			""")
	Set<ThreatAssessment> findLatestForAllRegisters();
}

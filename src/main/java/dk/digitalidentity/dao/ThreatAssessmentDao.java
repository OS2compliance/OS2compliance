package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;

import java.util.List;

public interface ThreatAssessmentDao extends JpaRepository<ThreatAssessment, Long> {

    long countByThreatCatalogsContains(final ThreatCatalog threatCatalog);
    ThreatAssessment findByThreatAssessmentReportS3DocumentId(long id);

	@Query("""
       SELECT ta FROM Asset a, Relation r, ThreatAssessment ta
       WHERE (
           (r.relationAId = a.id AND r.relationAType = 'ASSET' AND
            r.relationBType = 'THREAT_ASSESSMENT' AND ta.id = r.relationBId)
           OR
           (r.relationBId = a.id AND r.relationBType = 'ASSET' AND
            r.relationAType = 'THREAT_ASSESSMENT' AND ta.id = r.relationAId)
       )
       AND ta.createdAt >= :startDate
       AND ta.createdAt <= :endDate
       AND ta.createdAt = (
           SELECT MAX(ta2.createdAt)
           FROM Relation r2, ThreatAssessment ta2
           WHERE (
               (r2.relationAId = a.id AND r2.relationAType = 'ASSET' AND
                r2.relationBType = 'THREAT_ASSESSMENT' AND ta2.id = r2.relationBId)
               OR
               (r2.relationBId = a.id AND r2.relationBType = 'ASSET' AND
                r2.relationAType = 'THREAT_ASSESSMENT' AND ta2.id = r2.relationAId)
           )
           AND ta2.createdAt >= :startDate
           AND ta2.createdAt <= :endDate
       )
       """)
	Set<ThreatAssessment> findLatestForAllAssetsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

	@Query("""
       SELECT ta FROM Register r, Relation rel, ThreatAssessment ta
       WHERE (
           (rel.relationAId = r.id AND rel.relationAType = 'REGISTER' AND
            rel.relationBType = 'THREAT_ASSESSMENT' AND ta.id = rel.relationBId)
           OR
           (rel.relationBId = r.id AND rel.relationBType = 'REGISTER' AND
            rel.relationAType = 'THREAT_ASSESSMENT' AND ta.id = rel.relationAId)
       )
       AND ta.createdAt >= :startDate
       AND ta.createdAt <= :endDate
       AND ta.createdAt = (
           SELECT MAX(ta2.createdAt)
           FROM Relation rel2, ThreatAssessment ta2
           WHERE (
               (rel2.relationAId = r.id AND rel2.relationAType = 'REGISTER' AND
                rel2.relationBType = 'THREAT_ASSESSMENT' AND ta2.id = rel2.relationBId)
               OR
               (rel2.relationBId = r.id AND rel2.relationBType = 'REGISTER' AND
                rel2.relationAType = 'THREAT_ASSESSMENT' AND ta2.id = rel2.relationAId)
           )
           AND ta2.createdAt >= :startDate
           AND ta2.createdAt <= :endDate
       )
       """)
	Set<ThreatAssessment> findLatestForAllRegistersBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

	Set<ThreatAssessment> findByThreatAssessmentTypeInAndCreatedAtBetween(
			Collection<ThreatAssessmentType> threatAssessmentTypes,
			LocalDateTime startDate,
			LocalDateTime endDate
	);
	List<ThreatAssessment> findByDeletedFalseAndThreatAssessmentTypeIn(List<ThreatAssessmentType> types);
	List<ThreatAssessment> findAllByDeletedFalse();
}

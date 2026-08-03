package dk.digitalidentity.dao;

import dk.digitalidentity.dao.grid.SearchRepository;
import dk.digitalidentity.model.entity.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IncidentDao extends JpaRepository<Incident, Long>, SearchRepository {

	@Query("SELECT i FROM Incident i WHERE i.createdAt > :from and i.createdAt < :to")
	Page<Incident> findAll(@Param("from") final LocalDateTime from, @Param("to") final LocalDateTime to, final Pageable pageable);

	List<Incident> findByResponses_IncidentField_IdAndCreatedAtAfterAndCreatedAtBefore(Long id, LocalDateTime createdAt, LocalDateTime createdAt1);

	List<Incident> findByResponses_IncidentField_IdAndCreatedAtAfterAndCreatedAtBeforeAndDraftFalse(Long id, LocalDateTime createdAt, LocalDateTime createdAt1);

	List<Incident> findAllById(Long id);

	@Query(value = "SELECT COUNT(DISTINCT i.id) FROM incidents i " +
			"JOIN relations r ON (r.relation_a_id = i.id AND r.relation_a_type = 'INCIDENT' AND r.relation_b_id = :assetId AND r.relation_b_type = 'ASSET') " +
			"   OR (r.relation_b_id = i.id AND r.relation_b_type = 'INCIDENT' AND r.relation_a_id = :assetId AND r.relation_a_type = 'ASSET') " +
			"WHERE i.deleted = false AND i.draft = false AND i.created_at >= :from",
			nativeQuery = true)
	long countIncidentsByAssetIdSince(@Param("assetId") long assetId, @Param("from") LocalDateTime from);

	@Query(value = "SELECT COUNT(DISTINCT i.id) FROM incidents i " +
			"JOIN relations r ON (r.relation_a_id = i.id AND r.relation_a_type = 'INCIDENT' AND r.relation_b_id IN (:assetIds) AND r.relation_b_type = 'ASSET') " +
			"   OR (r.relation_b_id = i.id AND r.relation_b_type = 'INCIDENT' AND r.relation_a_id IN (:assetIds) AND r.relation_a_type = 'ASSET') " +
			"WHERE i.deleted = false AND i.draft = false AND i.created_at >= :from",
			nativeQuery = true)
	long countIncidentsByAssetIdsSince(@Param("assetIds") List<Long> assetIds, @Param("from") LocalDateTime from);
}

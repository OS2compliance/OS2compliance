package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentDao extends JpaRepository<Incident, Long> {

    Page<Incident> findAll(final Pageable pageable);
    @Query(value = "SELECT i.id, i.version, i.created_by_uuid, i.relation_type, i.name, i.created_at as createdAt, i.created_at, i.created_by, i.updated_at, i.updated_by, i.deleted, i.localized_enums " +
        "FROM incidents i " +
        "LEFT JOIN incident_field_responses ifr on ifr.incident_id=i.id " +
        "JOIN incident_fields inf on inf.id = ifr.incident_field_id " +
        "LEFT JOIN users u on u.uuid in (ifr.answer_element_ids)" +
        "LEFT JOIN ous ou on ou.uuid in (ifr.answer_element_ids)" +
        "LEFT JOIN assets a on a.id in (ifr.answer_element_ids)" +
        "WHERE i.deleted = false " +
        "   AND inf.index_column_name is not null " +
        "   AND (ifr.answer_text like concat('%',:search,'%') " +
        "       OR i.name like concat('%',:search,'%') " +
        "       OR u.name like concat('%',:search,'%') " +
        "       OR ou.name like concat('%',:search,'%') " +
        "       OR a.name like concat('%',:search,'%') " +
        "       OR ifr.answer_choice_values like concat('%',:search,'%')) " +
        "GROUP BY i.id",
        nativeQuery = true
    )
    Page<Incident> searchAll(@Param("search") final String search, final Pageable pageable);
}

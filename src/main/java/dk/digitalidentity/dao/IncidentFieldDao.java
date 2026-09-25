package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.IncidentField;
import dk.digitalidentity.model.entity.enums.IncidentType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IncidentFieldDao extends CrudRepository<IncidentField, Long> {

    @Query("select max(sortKey) from IncidentField")
    Optional<Long> selectMaxSortKey();

    List<IncidentField> findAllByOrderBySortKeyAsc();

	List<IncidentField> findAllByObligatoryAnswerTrue();

    /**
     * The date fields the incident log can filter its from/to range on. Only obligatory fields
     * qualify: filtering on a field that may be unanswered would silently hide incidents.
     */
    @Query("SELECT f FROM IncidentField f WHERE f.incidentType = :incidentType "
        + "AND f.obligatoryAnswer = true ORDER BY f.sortKey")
    List<IncidentField> findObligatoryFieldsByType(@Param("incidentType") final IncidentType incidentType);
}

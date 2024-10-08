package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.IncidentField;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentFieldDao extends CrudRepository<IncidentField, Long> {

    @Query("select max(sortKey) from IncidentField")
    Optional<Long> selectMaxSortKey();

    List<IncidentField> findAllByOrderBySortKeyAsc();

    List<IncidentField> findAllByIndexColumnTrueOrderBySortKeyAsc();
}

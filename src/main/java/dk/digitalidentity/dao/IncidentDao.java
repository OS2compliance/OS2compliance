package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

public interface IncidentDao extends CrudRepository<Incident, Long> {

    Page<Incident> findAll(final Pageable pageable);

}

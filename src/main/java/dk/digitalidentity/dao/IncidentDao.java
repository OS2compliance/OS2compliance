package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Incident;
import org.springframework.data.repository.CrudRepository;

public interface IncidentDao extends CrudRepository<Incident, Long> {
}

package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.ThreatCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThreatCatalogDao extends JpaRepository<ThreatCatalog, String> {
}

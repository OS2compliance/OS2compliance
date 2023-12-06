package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThreatCatalogThreatDao extends JpaRepository<ThreatCatalogThreat, String> {
}

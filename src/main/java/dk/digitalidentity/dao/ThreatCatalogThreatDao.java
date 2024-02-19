package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThreatCatalogThreatDao extends JpaRepository<ThreatCatalogThreat, String> {

    List<ThreatCatalogThreat> findByThreatCatalog(final ThreatCatalog threatCatalog);

}

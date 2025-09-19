package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.ThreatCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface ThreatCatalogDao extends JpaRepository<ThreatCatalog, String> {

    List<ThreatCatalog> findAllByHiddenFalse();

	List<ThreatCatalog> findByIdentifierIn(Set<String> catalogIdentifiers);
}

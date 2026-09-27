package dk.digitalidentity.dao.grid;

import dk.digitalidentity.model.entity.grid.ThreatCatalogGrid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThreatCatalogGridDao extends JpaRepository<ThreatCatalogGrid, String>, SearchRepository {
}

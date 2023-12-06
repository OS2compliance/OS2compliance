package dk.digitalidentity.dao.grid;

import dk.digitalidentity.model.entity.grid.RiskGrid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskGridDao extends JpaRepository<RiskGrid, Long>, SearchRepository {
    Page<RiskGrid> findAll(final Pageable pageable);
}

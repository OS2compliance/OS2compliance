package dk.digitalidentity.dao.grid;

import dk.digitalidentity.model.entity.grid.InactiveResponsibleGrid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InactiveResponsibleGridDao extends JpaRepository<InactiveResponsibleGrid, String>, SearchRepository {
    Page<InactiveResponsibleGrid> findAll(final Pageable pageable);
}

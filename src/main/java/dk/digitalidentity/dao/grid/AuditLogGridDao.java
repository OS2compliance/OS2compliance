package dk.digitalidentity.dao.grid;

import dk.digitalidentity.model.entity.grid.AuditLogGrid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogGridDao extends JpaRepository<AuditLogGrid, Long>, SearchRepository {
	Page<AuditLogGrid> findAll(final Pageable pageable);
}

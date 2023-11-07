package dk.digitalidentity.dao.grid;

import dk.digitalidentity.model.entity.grid.SupplierGrid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierGridDao extends JpaRepository<SupplierGrid, Long>, SearchRepository {
	Page<SupplierGrid> findAll(Pageable g);
}

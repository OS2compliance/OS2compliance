package dk.digitalidentity.dao.grid;

import dk.digitalidentity.model.entity.grid.RegisterGrid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegisterGridDao extends JpaRepository<RegisterGrid, Long>,  SearchRepository {
    Page<RegisterGrid> findAll(final Pageable pageable);
    Page<RegisterGrid> findAllByResponsibleUserUuidsContaining(String uuid, final Pageable pageable);

}

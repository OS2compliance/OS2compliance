package dk.digitalidentity.dao.grid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.model.entity.grid.DBSOversightGrid;

public interface DBSOversightGridDao extends JpaRepository<DBSOversightGrid, Long>, SearchRepository {
    Page<DBSOversightGrid> findAll(final Pageable pageable);

}

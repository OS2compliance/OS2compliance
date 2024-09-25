package dk.digitalidentity.dao.grid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.model.entity.grid.DBSAssetGrid;

public interface DBSAssetGridDao extends JpaRepository<DBSAssetGrid, Long>, SearchRepository {
    Page<DBSAssetGrid> findAll(final Pageable pageable);

}

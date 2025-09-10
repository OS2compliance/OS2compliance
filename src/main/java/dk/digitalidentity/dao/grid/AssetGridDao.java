package dk.digitalidentity.dao.grid;

import dk.digitalidentity.model.entity.grid.AssetGrid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetGridDao extends JpaRepository<AssetGrid, Long>, SearchRepository {
	Page<AssetGrid> findAll(final Pageable pageable);
    Page<AssetGrid> findAllByResponsibleUserUuidsContaining(String uuid, final Pageable pageable);


}

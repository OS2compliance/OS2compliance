package dk.digitalidentity.dao.grid;

import dk.digitalidentity.model.entity.grid.DPIAGrid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DPIAGridDao extends JpaRepository<DPIAGrid, Long>, SearchRepository{
}

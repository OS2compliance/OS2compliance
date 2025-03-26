package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.DPIA;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DPIADao extends JpaRepository<DPIA, Long> {
}

package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.DataProcessing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataProcessingDao extends JpaRepository<DataProcessing, Long> {
}

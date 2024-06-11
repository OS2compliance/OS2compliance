package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Precaution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrecautionDao extends JpaRepository<Precaution, Long> {
    List<Precaution> findByDeletedFalse();
}

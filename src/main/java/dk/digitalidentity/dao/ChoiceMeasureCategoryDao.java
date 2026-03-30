package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.ChoiceMeasureCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChoiceMeasureCategoryDao extends JpaRepository<ChoiceMeasureCategory, Long> {
	Optional<ChoiceMeasureCategory> findByName(final String name);
}
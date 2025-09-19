package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.StandardTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StandardTemplateDao extends JpaRepository<StandardTemplate, String> {
    StandardTemplate findByIdentifier(String identifier);

	List<StandardTemplate> findAllBySupportingIsTrue();
}

package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.StandardTemplate;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StandardTemplateSectionDao extends JpaRepository<StandardTemplateSection, String> {

    List<StandardTemplateSection> findByParent(final StandardTemplateSection parent);
    Optional<StandardTemplateSection> findByIdentifier(final String identifier);

    List<StandardTemplateSection> findByStandardTemplate(final StandardTemplate standardTemplate);

}

package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.StandardTemplate;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StandardTemplateSectionDao extends JpaRepository<StandardTemplateSection, String> {

    List<StandardTemplateSection> findByParent(final StandardTemplateSection parent);

    List<StandardTemplateSection> findByStandardTemplate(final StandardTemplate standardTemplate);

    List<StandardTemplateSection> findByIdentifierStartsWith(final String identifierPrefix);

	List<StandardTemplateSection> findByParentOrderBySortKey(StandardTemplateSection parent);
}

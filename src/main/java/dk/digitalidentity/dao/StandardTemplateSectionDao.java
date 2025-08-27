package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.StandardTemplate;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import dk.digitalidentity.model.entity.enums.StandardSectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StandardTemplateSectionDao extends JpaRepository<StandardTemplateSection, String> {

    List<StandardTemplateSection> findByParent(final StandardTemplateSection parent);

    List<StandardTemplateSection> findByStandardTemplate(final StandardTemplate standardTemplate);

    List<StandardTemplateSection> findByIdentifierStartsWith(final String identifierPrefix);

	List<StandardTemplateSection> findByParentOrderBySortKey(StandardTemplateSection parent);

	@Query("select sts from StandardTemplateSection sts " +
			"join sts.standardSection ss " +
			"where sts.parent.standardTemplate =:standardTemplate " +
			"and ss.selected = true")
	List<StandardTemplateSection> findSelectedChildSectionsByTemplate(@Param("standardTemplate") StandardTemplate standardTemplate);

	@Query("select COUNT(sts) from StandardTemplateSection sts " +
			"join sts.standardSection ss " +
			"where sts.parent.standardTemplate = :template " +
			"and ss.selected = true " +
			"and ss.status = :status")
	long countByTemplateAndStatus(@Param("template") StandardTemplate template,
			@Param("status") StandardSectionStatus status);
}

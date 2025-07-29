package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StandardSectionDao extends JpaRepository<StandardSection, Long> {

    Optional<StandardSection> findByTemplateSectionIdentifier(final String identifier);

    @Query("select ss from StandardTemplate st " +
        "left join StandardTemplateSection sts on sts.standardTemplate=st " +
        "inner join StandardSection ss on ss.templateSection=sts " +
        "where st.identifier=:templateIdentifier")
    List<StandardSection> findSectionsForStandardTemplate(@Param("templateIdentifier") final String templateIdentifier);

    List<StandardSection> findAllByTemplateSection(StandardTemplateSection templateSection);
}

package dk.digitalidentity.service;

import dk.digitalidentity.dao.StandardSectionDao;
import dk.digitalidentity.dao.StandardTemplateDao;
import dk.digitalidentity.dao.StandardTemplateSectionDao;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.StandardTemplate;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static dk.digitalidentity.util.NullSafe.nullSafe;

@Service
@Slf4j
public class StandardsService {
    private final StandardSectionDao standardSectionDao;
    private final StandardTemplateDao standardTemplateDao;
    private final StandardTemplateSectionDao standardTemplateSectionDao;

    public StandardsService(final StandardSectionDao standardSectionDao, final StandardTemplateDao standardTemplateDao, final StandardTemplateSectionDao standardTemplateSectionDao) {
        this.standardSectionDao = standardSectionDao;
        this.standardTemplateDao = standardTemplateDao;
        this.standardTemplateSectionDao = standardTemplateSectionDao;
    }

    // Get all sections that are linked to a specific iso27001 topic, eg. "4"
    public List<StandardSection> getSectionsForTopic(final String topic) {
        final StandardTemplate standardTemplate = standardTemplateDao.findByIdentifier("iso27001");
        final List<StandardTemplateSection> standardTemplates = standardTemplateSectionDao.findByStandardTemplate(standardTemplate);
        return standardTemplates.stream()
            .filter(s -> s.getSection().equals(topic))
            .flatMap(s -> standardTemplateSectionDao.findByParent(s).stream())
            .map(StandardTemplateSection::getStandardSection)
            .collect(Collectors.toList());
    }

    public String findTopSectionNumber(final StandardSection section) {
        StandardTemplateSection currentTemplate = section.getTemplateSection();
        while (currentTemplate.getParent() != null) {
            currentTemplate = currentTemplate.getParent();
        }
        return currentTemplate.getSection();
    }

    public String findStandardTemplateIdentifier(final StandardSection section) {
        final String name = nullSafe(() -> section.getTemplateSection().getStandardTemplate().getIdentifier());
        if (name != null) {
            return name;
        }
        return nullSafe(() -> section.getTemplateSection().getParent().getStandardTemplate().getIdentifier());
    }

}

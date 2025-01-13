package dk.digitalidentity.service;

import dk.digitalidentity.dao.StandardSectionDao;
import dk.digitalidentity.dao.StandardTemplateDao;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.StandardTemplate;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SupportingStandardService {
    private final StandardSectionDao standardSectionDao;
    private final StandardTemplateDao standardTemplateDao;

    public Optional<StandardTemplate> lookup(final String identifierOrId) {
        StandardTemplate template = standardTemplateDao.findByIdentifier(identifierOrId);
        if (template == null && StringUtils.isNumeric(identifierOrId)) {
            template = standardSectionDao.findById(Long.valueOf(identifierOrId))
                .map(s -> findSupportStandardRoot(s).getStandardTemplate())
                .orElse(null);
        }
        return Optional.ofNullable(template);
    }

    private StandardTemplateSection findSupportStandardRoot(final StandardSection section) {
        StandardTemplateSection found = section.getTemplateSection();
        while (found != null && found.getParent() != null) {
            found = found.getParent();
        }
        return found;
    }

}

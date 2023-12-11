package dk.digitalidentity.service.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.digitalidentity.dao.StandardSectionDao;
import dk.digitalidentity.dao.StandardTemplateDao;
import dk.digitalidentity.dao.StandardTemplateSectionDao;
import dk.digitalidentity.mapping.StandardMapper;
import dk.digitalidentity.model.dto.StandardTemplateDTO;
import dk.digitalidentity.model.dto.StandardTemplateSectionDTO;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import dk.digitalidentity.model.entity.enums.StandardSectionStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

@Service
@Slf4j
public class StandardTemplateImporter {
    private final ObjectMapper objectMapper;
    private final StandardMapper standardMapper;
    private final StandardTemplateDao standardTemplateDao;
    private final StandardSectionDao standardSectionDao;
    private final StandardTemplateSectionDao standardTemplateSectionDao;

    public StandardTemplateImporter(final ObjectMapper objectMapper, final StandardMapper standardMapper, final StandardTemplateDao standardTemplateDao, final StandardSectionDao standardSectionDao, final StandardTemplateSectionDao standardTemplateSectionDao) {
        this.objectMapper = objectMapper;
        this.standardMapper = standardMapper;
        this.standardTemplateDao = standardTemplateDao;
        this.standardSectionDao = standardSectionDao;
        this.standardTemplateSectionDao = standardTemplateSectionDao;
    }

    public void importStandardTemplate(final String filename) throws IOException {
        log.info("Importing choice list " + filename);
        final InputStream inputStream = new ClassPathResource(filename).getInputStream();
        final String jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        final StandardTemplateDTO standard = objectMapper.readValue(jsonString, StandardTemplateDTO.class);
        if (!standardTemplateDao.existsById(standard.getIdentifier())) {
            standardTemplateDao.save(standardMapper.fromDTO(standard));
        }
    }

    public void importStandardSections(final String filename) throws IOException {
        final StandardTemplateSectionDTO[] sections = objectMapper.readValue(new ClassPathResource(filename).getInputStream(), StandardTemplateSectionDTO[].class);
        Arrays.stream(sections)
                .filter(v -> !standardTemplateSectionDao.existsById(v.getIdentifier()))
                .map(sec -> {
                    final StandardTemplateSection entity = new StandardTemplateSection();
                    entity.setIdentifier(sec.getIdentifier());
                    entity.setDescription(sec.getDescription());
                    entity.setSecurityLevel(sec.getSecurityLevel());
                    entity.setSection(sec.getSection());
                    entity.setSortKey(sec.getSortKey());
                    if (sec.getParentIdentifier() != null) {
                        entity.setParent(standardTemplateSectionDao.getReferenceById(sec.getParentIdentifier()));
                    }
                    if (sec.getStandardIdentifier() != null) {
                        entity.setStandardTemplate(standardTemplateDao.getReferenceById(sec.getStandardIdentifier()));
                    }
                    return entity;
                })
                .forEach(standardTemplateSectionDao::save);
        for (final StandardTemplateSectionDTO templateSection : sections) {
            final Optional<StandardSection> foundSection = standardSectionDao.findByTemplateSectionIdentifier(templateSection.getIdentifier());
            if (foundSection.isEmpty()) {
                final StandardTemplateSection templateSectionEntity = standardTemplateSectionDao.findById(templateSection.getIdentifier())
                    .orElseThrow(() -> new RuntimeException("Could not find template section"));
                final StandardSection section = new StandardSection();
                section.setStatus(StandardSectionStatus.NOT_STARTED);
                section.setName(calculateName(templateSectionEntity));
                section.setTemplateSection(templateSectionEntity);
                standardSectionDao.save(section);
            }
        }
    }

    public void updateStandardSections(final String filename) throws IOException {
        final StandardTemplateSectionDTO[] sections = objectMapper.readValue(new ClassPathResource(filename).getInputStream(), StandardTemplateSectionDTO[].class);
        Arrays.stream(sections).forEach( newSection -> {
         StandardTemplateSection oldSection = standardTemplateSectionDao.findById(newSection.getIdentifier()).orElseThrow();
         oldSection.setSection(newSection.getSection());
         standardTemplateSectionDao.save(oldSection);
        } );
    }

    private static String calculateName(final StandardTemplateSection templateSectionEntity) {
        final String fullDesc = templateSectionEntity.getSection() + " " + templateSectionEntity.getDescription();
        return StringUtils.truncate(fullDesc, 150);
    }

}

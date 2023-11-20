package dk.digitalidentity.service.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.digitalidentity.dao.ThreatCatalogDao;
import dk.digitalidentity.dao.ThreatCatalogThreatDao;
import dk.digitalidentity.mapping.ThreatMapper;
import dk.digitalidentity.model.dto.ThreatCatalogDTO;
import dk.digitalidentity.model.dto.ThreatCatalogThreatDTO;
import dk.digitalidentity.model.entity.ThreatCatalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Service
@Slf4j
public class ThreatCatalogImporter {
    private final ObjectMapper objectMapper;
    private final ThreatCatalogThreatDao threatCatalogThreatDao;
    private final ThreatCatalogDao threatCatalogDao;
    private final ThreatMapper threatMapper;

    public ThreatCatalogImporter(final ObjectMapper objectMapper, final ThreatCatalogThreatDao threatCatalogThreatDao, final ThreatCatalogDao threatCatalogDao, final ThreatMapper threatMapper) {
        this.objectMapper = objectMapper;
        this.threatCatalogThreatDao = threatCatalogThreatDao;
        this.threatCatalogDao = threatCatalogDao;
        this.threatMapper = threatMapper;
    }

    public void importThreats(final String filename) throws IOException {
        final ThreatCatalogThreatDTO[] values = objectMapper.readValue(new ClassPathResource(filename).getInputStream(), ThreatCatalogThreatDTO[].class);
        Arrays.stream(values)
                .filter(v -> !threatCatalogThreatDao.existsById(v.getIdentifier()))
                .map(threatMapper::fromDTO)
                .forEach(threatCatalogThreatDao::save);

    }

    public void importCatalog(final String filename) throws IOException {
        log.info("Importing threat catalog " + filename);
        final InputStream inputStream = new ClassPathResource(filename).getInputStream();
        final String jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        final ThreatCatalogDTO catalog = objectMapper.readValue(jsonString, ThreatCatalogDTO.class);
        if (!threatCatalogDao.existsById(catalog.getIdentifier())) {
            final ThreatCatalog entity = threatMapper.fromDTO(catalog);
            threatCatalogDao.save(entity);
        }
    }
}

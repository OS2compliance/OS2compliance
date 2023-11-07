package dk.digitalidentity.mapping;

import dk.digitalidentity.dao.ThreatCatalogDao;
import dk.digitalidentity.model.entity.ThreatCatalog;
import org.springframework.stereotype.Component;

@Component
public class ThreatMapperResolver {
    private final ThreatCatalogDao threatCatalogDao;

    public ThreatMapperResolver(final ThreatCatalogDao threatCatalogDao) {
        this.threatCatalogDao = threatCatalogDao;
    }

    public ThreatCatalog resolve(final String reference) {
        return threatCatalogDao.findById(reference).orElse(null);
    }

}

package dk.digitalidentity.service;

import dk.digitalidentity.dao.ThreatCatalogDao;
import dk.digitalidentity.dao.ThreatCatalogThreatDao;
import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final ThreatCatalogDao threatCatalogDao;
    private final ThreatCatalogThreatDao threatCatalogThreatDao;

    public List<ThreatCatalog> findAll() {
        return threatCatalogDao.findAll();
    }

    public Optional<ThreatCatalog> get(final String identifier) {
        return threatCatalogDao.findById(identifier);
    }

    public ThreatCatalog save(final ThreatCatalog threatCatalog) {
        return threatCatalogDao.save(threatCatalog);
    }

    public ThreatCatalogThreat saveThreat(final ThreatCatalogThreat threat) {
        return threatCatalogThreatDao.save(threat);
    }

    public Optional<ThreatCatalogThreat> getThreat(final String identifier) {
        return threatCatalogThreatDao.findById(identifier);
    }

}

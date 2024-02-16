package dk.digitalidentity.service;

import dk.digitalidentity.dao.ThreatCatalogDao;
import dk.digitalidentity.model.entity.ThreatCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final ThreatCatalogDao threatCatalogDao;

    public List<ThreatCatalog> findAll() {
        return threatCatalogDao.findAll();
    }

    public Optional<ThreatCatalog> get(final String identifier) {
        return threatCatalogDao.findById(identifier);
    }

}

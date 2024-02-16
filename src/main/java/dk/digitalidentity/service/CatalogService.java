package dk.digitalidentity.service;

import dk.digitalidentity.dao.ThreatCatalogDao;
import dk.digitalidentity.model.entity.ThreatCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final ThreatCatalogDao threatCatalogDao;

    public List<ThreatCatalog> findAll() {
        return threatCatalogDao.findAll();
    }

}

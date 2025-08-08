package dk.digitalidentity.service;

import dk.digitalidentity.dao.ThreatAssessmentDao;
import dk.digitalidentity.dao.ThreatAssessmentResponseDao;
import dk.digitalidentity.dao.ThreatCatalogDao;
import dk.digitalidentity.dao.ThreatCatalogThreatDao;
import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final ThreatCatalogDao threatCatalogDao;
    private final ThreatCatalogThreatDao threatCatalogThreatDao;
    private final ThreatAssessmentResponseDao threatAssessmentResponseDao;
    private final ThreatAssessmentDao threatAssessmentDao;

    public List<ThreatCatalog> findAll() {
        return threatCatalogDao.findAll();
    }

    public List<ThreatCatalog> findAllVisible() {
        return threatCatalogDao.findAllByHiddenFalse();
    }

    public Optional<ThreatCatalog> get(final String identifier) {
        return threatCatalogDao.findById(identifier);
    }

    public ThreatCatalog save(final ThreatCatalog threatCatalog) {
        return threatCatalogDao.save(threatCatalog);
    }

    public void delete(final ThreatCatalog threatCatalog) {
        threatCatalogDao.delete(threatCatalog);
    }

    public ThreatCatalogThreat saveThreat(final ThreatCatalogThreat threat) {
        return threatCatalogThreatDao.save(threat);
    }

    public Optional<ThreatCatalogThreat> getThreat(final String identifier) {
        return threatCatalogThreatDao.findById(identifier);
    }

    public boolean threatInUse(final ThreatCatalogThreat threat) {
        return threatAssessmentResponseDao.countByThreatCatalogThreat(threat) > 0;
    }

    public boolean inUse(final ThreatCatalog threatCatalog) {
        return threatAssessmentDao.countByThreatCatalogsContains(threatCatalog) > 0;
    }

    @Transactional
    public void deleteThreat(final ThreatCatalogThreat threat) {
        threatCatalogThreatDao.delete(threat);
    }

    @Transactional
    public void copyToNew(final ThreatCatalog source) {
        final ThreatCatalog catalog = new ThreatCatalog();
        catalog.setIdentifier(UUID.randomUUID().toString());
        catalog.setName(source.getName());
        catalog.setHidden(source.isHidden());
        final ThreatCatalog savedCatalog = threatCatalogDao.save(catalog);
        final List<ThreatCatalogThreat> copiedThreats = threatCatalogThreatDao.findByThreatCatalog(source).stream()
            .map(t -> copyThreatTo(t, savedCatalog))
            .collect(Collectors.toList());
        savedCatalog.setThreats(copiedThreats);
    }

    private ThreatCatalogThreat copyThreatTo(final ThreatCatalogThreat source, final ThreatCatalog destination) {
        final ThreatCatalogThreat threat = new ThreatCatalogThreat();
        threat.setIdentifier(UUID.randomUUID().toString());
        threat.setThreatCatalog(destination);
        threat.setSortKey(source.getSortKey());
        threat.setDescription(source.getDescription());
        threat.setRights(source.isRights());
        threat.setThreatType(source.getThreatType());
        return threatCatalogThreatDao.save(threat);
    }
}

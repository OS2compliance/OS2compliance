package dk.digitalidentity.service;

import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.dao.DataProcessingDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetSupplierMapping;
import dk.digitalidentity.model.entity.DataProcessing;
import dk.digitalidentity.model.entity.DataProtectionImpactAssessment;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.TransferImpactAssessment;
import dk.digitalidentity.model.entity.enums.RelationType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AssetService {
    private final AssetDao assetDao;
    private final RelationService relationService;
    private final DataProcessingDao dataProcessingDao;

    public Optional<Asset> get(final Long id) {
        return assetDao.findById(id);
    }

    public Page<Asset> getPaged(final int pageSize, final int page) {
        return assetDao.findAll(Pageable.ofSize(pageSize).withPage(page));
    }

    public List<Asset> findAllById(final Iterable<Long> ids) {
        return assetDao.findAllById(ids);
    }

    public Optional<Asset> findById(final Long id) {
        return assetDao.findById(id);
    }

    public Asset create(final Asset asset) {
        final Asset saved = assetDao.save(asset);
        if (saved.getDpia() == null) {
            saved.setDpia(new DataProtectionImpactAssessment());
            saved.getDpia().setAsset(saved);
        }
        if (saved.getDataProcessing() == null) {
            saved.setDataProcessing(new DataProcessing());
        }
        if (saved.getTia() == null) {
            saved.setTia(new TransferImpactAssessment());
            saved.getTia().setAsset(asset);
        }
        addDefaultSubSupplier(saved);
        return saved;
    }

    public void update(final Asset asset) {
        addDefaultSubSupplier(asset);
        assetDao.saveAndFlush(asset);
    }

    public void delete(final Asset asset) {
        assetDao.delete(asset);
    }

    public Optional<Asset> findByProperty(final String key, final String value) {
        return assetDao.findByPropertyValue(key, value);
    }

    public List<Asset> findAllByRelations(final List<Relation> relations) {
        final List<Long> lookupIds = relations.stream()
            .map(r -> r.getRelationAType() == RelationType.ASSET
                ? r.getRelationAId()
                : r.getRelationBId())
            .toList();
        return assetDao.findAllById(lookupIds);
    }

    public List<Asset> findRelatedTo(final Register register) {
        final List<Relation> relations = relationService.findRelatedToWithType(register, RelationType.ASSET);
        return findAllByRelations(relations);
    }

    public List<Asset> findBySupplier(final Supplier supplier) {
        return assetDao.findBySupplier(supplier);
    }

    /**
     * Will find the main supplier {@link AssetSupplierMapping} if none is found a default placeholder will be returned
     */
    public AssetSupplierMapping findMainSupplier(final Asset asset) {
        if (asset.getSupplier() == null) {
            return AssetSupplierMapping.builder().asset(asset).build();
        }
        return asset.getSuppliers().stream()
            .filter(s -> Objects.equals(s.getSupplier().getId(), asset.getSupplier().getId()))
            .findFirst().orElse(AssetSupplierMapping.builder().asset(asset).build());
    }


    /** if NextInspection is of type DATE it just returns the inputted value*/
    public LocalDate getNextInspectionByInterval(final Asset asset, final LocalDate date) {
        if (asset.getNextInspection() == null) {
            return null;
        }
        return switch (asset.getNextInspection()) {
            case DATE -> //should never actually hit this case as we check prior.
                date;
            case MONTH -> date.plusMonths(1);
            case QUARTER -> date.plusMonths(3);
            case HALF_YEAR -> date.plusMonths(6);
            case YEAR -> date.plusYears(1);
            case EVERY_2_YEARS -> date.plusYears(2);
            case EVERY_3_YEARS -> date.plusYears(3);
        };
    }

    @Transactional
    public void deleteById(final Asset asset) {
        relationService.deleteRelatedTo(asset.getId());
        dataProcessingDao.delete(asset.getDataProcessing());
        assetDao.delete(asset);
    }


    private static void addDefaultSubSupplier(final Asset saved) {
        if (saved.getSupplier() != null && saved.getSuppliers().isEmpty()) {
            saved.getSuppliers().add(AssetSupplierMapping.builder()
                .asset(saved)
                .supplier(saved.getSupplier())
                .build());
        }
    }

    public Asset save(final Asset asset) {
        return assetDao.save(asset);
    }
}

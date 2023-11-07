package dk.digitalidentity.service;

import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetSupplierMapping;
import dk.digitalidentity.model.entity.DataProcessing;
import dk.digitalidentity.model.entity.DataProtectionImpactAssessment;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.TransferImpactAssessment;
import dk.digitalidentity.model.entity.enums.RelationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static dk.digitalidentity.integration.kitos.KitosConstants.KITOS_UUID_PROPERTY_KEY;

@Service
public class AssetService {
    private final AssetDao assetDao;
    private final RelationDao relationDao;

    public AssetService(final AssetDao assetDao, final RelationDao relationDao) {
        this.assetDao = assetDao;
        this.relationDao = relationDao;
    }

    public Optional<Asset> get(final Long id) {
        return assetDao.findById(id);
    }

    public Page<Asset> getPaged(final int pageSize, final int page) {
        return assetDao.findAll(Pageable.ofSize(pageSize).withPage(page));
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

    public Optional<Asset> findByKitosUuid(final String kitosUuid) {
        return assetDao.findByPropertyValue(KITOS_UUID_PROPERTY_KEY, kitosUuid);
    }

    public List<Asset> findRelatedTo(final Register register) {
        return relationDao.findRelatedToWithType(register.getId(), RelationType.ASSET).stream()
            .map(r -> r.getRelationAType() == RelationType.ASSET ? r.getRelationAId() : r.getRelationBId())
            .map(rid -> assetDao.findById(rid).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
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

    public void deleteById(final Long id) {
        assetDao.deleteById(id);
    }


    private static void addDefaultSubSupplier(final Asset saved) {
        if (saved.getSupplier() != null && saved.getSuppliers() == null || saved.getSuppliers().isEmpty()) {
            saved.getSuppliers().add(AssetSupplierMapping.builder()
                .asset(saved)
                .supplier(saved.getSupplier())
                .build());
        }
    }

}

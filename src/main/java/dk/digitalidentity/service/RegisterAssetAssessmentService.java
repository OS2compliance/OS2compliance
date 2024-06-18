package dk.digitalidentity.service;

import dk.digitalidentity.model.dto.RegisterAssetRiskDTO;
import dk.digitalidentity.model.dto.RelationDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetSupplierMapping;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.enums.RelationType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.ASSET_ASSESSMENT_PROPERTY;
import static dk.digitalidentity.Constants.RISK_SCALE_PROPERTY_NAME;

@Service
@RequiredArgsConstructor
public class RegisterAssetAssessmentService {
    private static final Logger log = LoggerFactory.getLogger(RegisterAssetAssessmentService.class);
    private final AssetService assetService;
    private final RelationService relationService;
    private final ThreatAssessmentService threatAssessmentService;
    private final RegisterService registerService;
    private final ScaleService scaleService;
    private final PropertyResolver propertyResolver;
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * This method takes a list of related assets and transforms it into a list of pairs.
     * Each pair consists of the value of "RISK_SCALE_PROPERTY_NAME" (or a default value of 100 if not available) and
     * the primary supplier for the asset.
     *
     * @param relatedAssets a list of RelationDTO objects representing the related assets
     * @return a list of pairs, where each pair contains an integer value representing the risk scale property and an AssetSupplierMapping object representing the primary supplier
     *  for the asset
     */
    @Transactional
    public List<Pair<Integer, AssetSupplierMapping>> assetSupplierMappingList(final List<RelationDTO<Register, Relatable>> relatedAssets) {
        return relatedAssets.stream()
            .map(r -> Pair.of(r.getProperties().entrySet().stream()
                .filter(p -> p.getKey().equals(RISK_SCALE_PROPERTY_NAME)).findFirst().map(v -> Integer.valueOf(v.getValue()))
                .orElse(100), (Asset)r.getB()))
            .map(r -> Pair.of(r.getLeft(), assetService.findMainSupplier(r.getRight())))
            .collect(Collectors.toList());
    }

    /**
     * This method takes a list of asset-supplier mappings and calculates the risk assessments for each asset.
     *
     * @param assetSupplierMappingList a list of pairs, where each pair consists of an integer value representing the risk scale property and an AssetSupplierMapping object representing
     *  the primary supplier for the asset
     * @return a list of RegisterAssetRiskDTO objects, each containing the threat assessment, date, risk score, weighted percentage, weighted assessment, and weighted risk score
     */
    @Transactional
    public List<RegisterAssetRiskDTO> assetThreatAssessments(final List<Pair<Integer, AssetSupplierMapping>> assetSupplierMappingList) {
        return assetSupplierMappingList.stream()
            .map(a -> threatAssessmentService.calculateRiskForRegistersRelatedAssets(a.getValue().getAsset(), a.getKey()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }

    @Transactional
    public void updateAllRelatedToThreatAssessment(final Long threatAssessmentId) {
        entityManager.setFlushMode(FlushModeType.COMMIT);
        final List<Relation> relatedAssets = relationService.findRelatedToWithType(Collections.singletonList(threatAssessmentId), RelationType.ASSET);
        final Set<Long> assetIds = relatedAssets.stream().map(r -> r.getRelationAType() == RelationType.ASSET ? r.getRelationAId() : r.getRelationBId())
            .collect(Collectors.toSet());
        final Set<Long> registerIds = relationService.findRelatedToWithType(assetIds, RelationType.REGISTER).stream()
            .map(r -> r.getRelationAType() == RelationType.REGISTER ? r.getRelationAId() : r.getRelationBId())
            .collect(Collectors.toSet());
        registerIds.stream()
            .map(registerService::findById)
            .filter(Optional::isPresent)
            .forEach(r -> updateAssetAssessment(r.get()));
    }

    @Transactional
    public void updateAllRelatedToAsset(final Long assetId) {
        entityManager.setFlushMode(FlushModeType.COMMIT);
        final Set<Long> registerIds = relationService.findRelatedToWithType(Collections.singletonList(assetId), RelationType.REGISTER).stream()
            .map(r -> r.getRelationAType() == RelationType.REGISTER ? r.getRelationAId() : r.getRelationBId())
            .collect(Collectors.toSet());
        registerIds.stream()
            .map(registerService::findById)
            .filter(Optional::isPresent)
            .forEach(r -> updateAssetAssessment(r.get()));
    }

    @Transactional
    public void updateAssetAssessment(final Long registerId) {
        registerService.findById(registerId).ifPresent(this::updateAssetAssessment);
    }

    @Transactional
    public void updateAssetAssessmentAll() {
        registerService.findAll().forEach(this::updateAssetAssessment);
    }

    @Transactional
    public void updateAssetAssessment(final Register register) {
        entityManager.setFlushMode(FlushModeType.COMMIT);
        final long millis = System.currentTimeMillis();
        final List<RelationDTO<Register, Relatable>> relatedAssets = relationService.findRelations(register, RelationType.ASSET);
        final List<Pair<Integer, AssetSupplierMapping>> assetSupplierMappingList = assetSupplierMappingList(relatedAssets);
        final int score = assetSupplierMappingList.stream()
            .map(a -> threatAssessmentService.calculateRiskForRegistersRelatedAssets(a.getValue().getAsset(), a.getKey()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .mapToInt(r -> r.getWeightedRiskScore().intValue())
            .max().orElse(0);
        final Property property = register.getProperties().stream()
            .filter(p -> p.getKey().equals(ASSET_ASSESSMENT_PROPERTY))
            .findFirst().orElseGet(() -> Property.builder()
                .key(ASSET_ASSESSMENT_PROPERTY)
                .entity(register)
                .build());
        if (score >= 1) {
            property.setValue(scaleService.getRiskAssessmentForRisk(score).name());
            register.getProperties().add(property);
        } else {
            register.getProperties().remove(property);
        }
        log.info("updateAssetAssessment done, took {}ms", System.currentTimeMillis() - millis);
    }

}

package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetMeasure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetMeasuresDao extends JpaRepository<AssetMeasure, Long> {
    List<AssetMeasure> findByAsset(final Asset asset);

    Optional<AssetMeasure> findByAssetAndMeasureIdentifier(final Asset asset, String identifier);
}

package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetOversight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetOversightDao extends JpaRepository<AssetOversight, Long> {
    List<AssetOversight> findByAssetOrderByCreationDateDesc(Asset asset);
}

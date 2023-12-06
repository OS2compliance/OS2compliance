package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.AssetSupplierMapping;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AssetSupplierMappingDao extends CrudRepository<AssetSupplierMapping,Long> {
    List<AssetSupplierMapping> findAllByAssetIdIn(List<Long> ids);
}

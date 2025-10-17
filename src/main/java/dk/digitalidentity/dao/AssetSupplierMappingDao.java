package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.AssetSupplierMapping;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssetSupplierMappingDao extends CrudRepository<AssetSupplierMapping,Long> {
    List<AssetSupplierMapping> findAllByAssetIdIn(List<Long> ids);

	@Query("SELECT asm FROM AssetSupplierMapping asm " +
			"JOIN FETCH asm.asset a " +
			"WHERE asm.supplier.id = :supplierId " +
			"ORDER BY a.name")
	List<AssetSupplierMapping> findBySupplierIdWithAssets(@Param("supplierId") Long supplierId);
}

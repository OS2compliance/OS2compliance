package dk.digitalidentity.service;

import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.dao.AssetSupplierMappingDao;
import dk.digitalidentity.dao.SupplierDao;
import dk.digitalidentity.model.dto.AssetWithMappingsDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetSupplierMapping;
import dk.digitalidentity.model.entity.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetSupplierMappingService {
	private final AssetSupplierMappingDao assetSupplierMappingDao;
	private final SupplierDao supplierDao;
	private final AssetDao assetDao;

	public List<AssetWithMappingsDTO> getSupplierWithAssetMappings(Long supplierId) {
		if (supplierId == null) {
			return Collections.emptyList();
		}

		Supplier supplier = supplierDao.findById(supplierId).orElse(null);
		if (supplier == null) {
			return Collections.emptyList();
		}

		List<Asset> assets = assetDao.findBySupplierId(supplierId);

		List<AssetSupplierMapping> mappings = assetSupplierMappingDao.findBySupplierIdWithAssets(supplierId);

		Map<Long, List<AssetSupplierMapping>> mappingsByAsset = mappings.stream()
				.collect(Collectors.groupingBy(asm -> asm.getAsset().getId()));

		List<AssetWithMappingsDTO> result = new ArrayList<>();

		for (Asset asset : assets) {
			List<AssetSupplierMapping> assetMappings = mappingsByAsset.get(asset.getId());

			if (assetMappings != null && !assetMappings.isEmpty()) {
				for (AssetSupplierMapping mapping : assetMappings) {
					result.add(new AssetWithMappingsDTO(
							asset,
							supplier,
							mapping.getService(),
							mapping.getSupplier().getCountry(),
							mapping.getThirdCountryTransfer(),
							mapping.getAcceptanceBasis()
					));
				}
			}
			else {
				result.add(new AssetWithMappingsDTO(
						asset,
						supplier,
						null,
						null,
						null,
						null
				));
			}
		}
		return result;
	}
}

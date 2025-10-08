package dk.digitalidentity.util;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.enums.ThirdCountryTransfer;
import org.springframework.stereotype.Component;

@Component
public class AssetUtils {

	public boolean hasThirdCountryTransfer(Object relatable) {
		if (relatable instanceof Asset) {
			Asset asset = (Asset) relatable;
			return asset.getSuppliers().stream()
					.anyMatch(mapping -> mapping.getThirdCountryTransfer() == ThirdCountryTransfer.YES);
		}
		return false;
	}

	public boolean isAsset(Object relatable) {
		return relatable instanceof Asset;
	}
}

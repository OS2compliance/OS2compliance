package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.enums.ThirdCountryTransfer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetWithMappingsDTO {
	private Asset asset;
	private Supplier supplier;
	private String service;
	private String country;
	private ThirdCountryTransfer thirdCountryTransfer;
	private String acceptanceBasis;
}

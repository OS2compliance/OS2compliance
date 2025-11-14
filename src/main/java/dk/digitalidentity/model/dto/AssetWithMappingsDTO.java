package dk.digitalidentity.model.dto;

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
	private Long assetId;
	private String assetName;
	private Long supplierId;
	private String service;
	private ThirdCountryTransfer thirdCountryTransfer;
	private String acceptanceBasis;
}

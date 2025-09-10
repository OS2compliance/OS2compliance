package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.ExcelColumn;
import dk.digitalidentity.model.ExcludeFromExport;
import dk.digitalidentity.model.dto.enums.AllowedAction;
import dk.digitalidentity.model.dto.enums.AllowedAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetDTO {
    private Long id;
	@ExcelColumn(headerName = "Navn", order = 1)
    private String name;
	@ExcelColumn(headerName = "Leverandør", order = 2)
    private String supplier;
	@ExcelColumn(headerName = "Type", order = 4)
    private String assetType;
	@ExcelColumn(headerName = "Systemejer", order = 5)
    private String ownedByUsers;
	@ExcelColumn(headerName = "Opdateret", order = 6)
    private String updatedAt;
	@ExcelColumn(headerName = "Risiko vurdering", order = 8)
    private String assessment;
	@ExcludeFromExport
    private Integer assessmentOrder;
	@ExcelColumn(headerName = "Status", order = 9)
    private String assetStatus;
	@ExcludeFromExport
    private String assetCategory;
	@ExcludeFromExport
    private Integer assetCategoryOrder;
	@ExcludeFromExport
    private String kitos;
	@ExcelColumn(headerName = "Tredjelandsoverførsel", order = 3)
    private boolean hasThirdCountryTransfer;
	@ExcelColumn(headerName = "Antal beh.", order = 7)
    private int registers;
	@ExcludeFromExport
	private Set<AllowedAction> allowedActions;
	@ExcludeFromExport
	private boolean oldKitos;
	@ExcludeFromExport
	private boolean active;
	@ExcelColumn(headerName = "Systemansvarlig", order = 10)
	private String responsibleUsers;
}

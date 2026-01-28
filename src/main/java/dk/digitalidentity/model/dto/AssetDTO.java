package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.ExcelColumn;
import dk.digitalidentity.model.ExcludeFromExport;
import dk.digitalidentity.model.dto.enums.AllowedAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
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
	@ExcelColumn(headerName = "Sidste tilsyn", order = 11)
	private LocalDate lastOversightDate;
	@ExcludeFromExport
	private List<TagDTO> tags;

	// Risk assessment calculated fields
	@ExcludeFromExport
	private Double avgProbability;
	@ExcludeFromExport
	private Double avgConsequenceOverall;
	@ExcludeFromExport
	private Double avgConsequenceConfidentialityRegistered;
	@ExcludeFromExport
	private Double avgConsequenceConfidentialityOrganisation;
	@ExcludeFromExport
	private Double avgConsequenceConfidentialitySociety;
	@ExcludeFromExport
	private Double avgConsequenceIntegrityRegistered;
	@ExcludeFromExport
	private Double avgConsequenceIntegrityOrganisation;
	@ExcludeFromExport
	private Double avgConsequenceIntegritySociety;
	@ExcludeFromExport
	private Double avgConsequenceAvailabilityRegistered;
	@ExcludeFromExport
	private Double avgConsequenceAvailabilityOrganisation;
	@ExcludeFromExport
	private Double avgConsequenceAvailabilitySociety;
	@ExcludeFromExport
	private Double avgConsequenceAuthenticitySociety;
	@ExcludeFromExport
	private String threatTypeList;
	@ExcludeFromExport
	private String catalogList;
	@ExcludeFromExport
	private Double riskScore;
}

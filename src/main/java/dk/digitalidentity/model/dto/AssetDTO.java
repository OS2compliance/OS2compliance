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
	@ExcelColumn(headerName = "Gennemsnitlig sandsynlighed", order = 14)
	private Double avgProbability;
	@ExcelColumn(headerName = "Gennemsnitlig Konsekvens", order = 15)
	private Double avgConsequenceOverall;
	@ExcelColumn(headerName = "Konsekvens for den registrerede - fortrolighed", order = 16)
	private Double avgConsequenceConfidentialityRegistered;
	@ExcelColumn(headerName = "Konsekvens for organisationen - fortrolighed", order = 19)
	private Double avgConsequenceConfidentialityOrganisation;
	@ExcelColumn(headerName = "Konsekvens for samfundet - fortrolighed", order = 22)
	private Double avgConsequenceConfidentialitySociety;
	@ExcelColumn(headerName = "Konsekvens for den registrerede - integritet", order = 17)
	private Double avgConsequenceIntegrityRegistered;
	@ExcelColumn(headerName = "Konsekvens for organisationen - integritet", order = 20)
	private Double avgConsequenceIntegrityOrganisation;
	@ExcelColumn(headerName = "Konsekvens for samfundet - integritet", order = 23)
	private Double avgConsequenceIntegritySociety;
	@ExcelColumn(headerName = "Konsekvens for den registrerede - tilgængelighed", order = 18)
	private Double avgConsequenceAvailabilityRegistered;
	@ExcelColumn(headerName = "Konsekvens for organisationen - tilgængelighed", order = 21)
	private Double avgConsequenceAvailabilityOrganisation;
	@ExcelColumn(headerName = "Konsekvens for samfundet - tilgængelighed", order = 24)
	private Double avgConsequenceAvailabilitySociety;
	@ExcelColumn(headerName = "Konsekvens for samfundet - autenticitet", order = 25)
	private Double avgConsequenceAuthenticitySociety;
	@ExcelColumn(headerName = "Trusselstyper", order = 12)
	private String threatTypeList;
	@ExcelColumn(headerName = "Risikokataloger", order = 13)
	private String catalogList;
	@ExcludeFromExport
	private Double riskScore;
}

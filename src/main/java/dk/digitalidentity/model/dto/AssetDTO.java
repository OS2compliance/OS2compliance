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
	@ExcelColumn(headerName = "Risikovurdering", order = 8)
    private String assessment;
	@ExcludeFromExport
    private Integer assessmentOrder;
	@ExcelColumn(headerName = "Status", order = 9)
    private String assetStatus;
	@ExcelColumn(headerName = "Kategori", order = 27)
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

	@ExcelColumn(headerName = "Ansvarlige forvaltninger", order = 26)
	private String departments;
	@ExcelColumn(headerName = "Beskrivelse", order = 28)
	private String description;
	@ExcelColumn(headerName = "Driftsansvarlig", order = 29)
	private String operationResponsibleUsers;
	@ExcelColumn(headerName = "Kritikalitet", order = 30)
	private String criticality;
	@ExcludeFromExport
	private Integer criticalityOrder;
	@ExcelColumn(headerName = "Samfundskritisk", order = 31)
	private boolean sociallyCritical;
	@ExcelColumn(headerName = "Anvender løsningen AI", order = 32)
	private String aiStatus;
	@ExcelColumn(headerName = "Kontraktdato", order = 33)
	private LocalDate contractDate;
	@ExcelColumn(headerName = "Kontraktophør", order = 34)
	private LocalDate contractTermination;
	@ExcelColumn(headerName = "Opsigelsesvarsel", order = 35)
	private String terminationNotice;
	@ExcelColumn(headerName = "Er der indgået databehandleraftale", order = 36)
	private String dataProcessingAgreementStatus;
	@ExcelColumn(headerName = "Databehandleraftale dato", order = 37)
	private LocalDate dataProcessingAgreementDate;
	@ExcelColumn(headerName = "Vurdering af foranstaltninger", order = 38)
	private String securityMeasuresStatus;
	@ExcludeFromExport
	private Integer securityMeasuresStatusOrder;
	@ExcelColumn(headerName = "Risikovurdering fravalgt", order = 39)
	private String riskAssessmentOptOutStatus;
	@ExcludeFromExport
	private Integer riskAssessmentOptOutStatusOrder;
	@ExcelColumn(headerName = "DPIA", order = 40)
	private String dpiaStatus;
	@ExcludeFromExport
	private Integer dpiaStatusOrder;
	@ExcelColumn(headerName = "TIA vurdering", order = 41)
	private String tiaStatus;
	@ExcludeFromExport
	private Integer tiaStatusOrder;
	@ExcelColumn(headerName = "Systemet skal arkiveres", order = 42)
	private String archive;
}

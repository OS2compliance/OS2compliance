package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.dto.enums.AllowedAction;
import dk.digitalidentity.model.ExcelColumn;
import dk.digitalidentity.model.ExcludeFromExport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDTO {
    private Long id;
	@ExcelColumn(headerName = "Titel", order = 1)
    private String name;
	@ExcludeFromExport
    private String packageName;
	@ExcludeFromExport
    private String description;
	@ExcludeFromExport
    private String responsibleUsers;
	@ExcludeFromExport
    private String customResponsibleUserUuids;
	@ExcelColumn(headerName = "Afdeling", order = 2)
    private String responsibleOUs;
	@ExcludeFromExport
    private String departments;
	@ExcelColumn(headerName = "Opdateret", order = 4)
    private String updatedAt;
	@ExcelColumn(headerName = "Konsekvens vurdering", order = 5)
    private String consequence;
	@ExcludeFromExport
    private Integer consequenceOrder;
	@ExcelColumn(headerName = "Risiko vurdering", order = 6)
    private String risk;
	@ExcludeFromExport
    private Integer riskOrder;
	@ExcelColumn(headerName = "Status", order = 8)
    private String status;
	@ExcludeFromExport
    private Integer statusOrder;
	@ExcludeFromExport
    private Set<String> gdprChoices;
	@ExcelColumn(headerName = "Aktiver", order = 9)
    private int assetCount;
	@ExcelColumn(headerName = "Risiko aktiver", order = 7)
    private String assetAssessment;
	@ExcludeFromExport
    private Integer assetAssessmentOrder;
	@ExcludeFromExport
	@Builder.Default
	private Set<String> kleMainGroups = new HashSet<>();
	@ExcludeFromExport
	@Builder.Default
	private Set<String> kleGroups = new HashSet<>();
	@ExcludeFromExport
	@Builder.Default
	private Set<String> kleSubjects = new HashSet<>();
	@ExcludeFromExport
	@Builder.Default
	private List<TagDTO> tags = new ArrayList<>();
	@ExcludeFromExport
	@Builder.Default
	private Set<AllowedAction> allowedActions = new HashSet<>();

	// Risk assessment calculated fields
	@ExcelColumn(headerName = "Gennemsnitlig sandsynlighed", order = 12)
	private Double avgProbability;
	@ExcelColumn(headerName = "Gennemsnitlig Konsekvens", order = 13)
	private Double avgConsequenceOverall;
	@ExcelColumn(headerName = "Konsekvens for den registrerede - fortrolighed", order = 14)
	private Double avgConsequenceConfidentialityRegistered;
	@ExcelColumn(headerName = "Konsekvens for organisationen - fortrolighed", order = 17)
	private Double avgConsequenceConfidentialityOrganisation;
	@ExcelColumn(headerName = "Konsekvens for samfundet - fortrolighed", order = 20)
	private Double avgConsequenceConfidentialitySociety;
	@ExcelColumn(headerName = "Konsekvens for den registrerede - integritet", order = 15)
	private Double avgConsequenceIntegrityRegistered;
	@ExcelColumn(headerName = "Konsekvens for organisationen - integritet", order = 18)
	private Double avgConsequenceIntegrityOrganisation;
	@ExcelColumn(headerName = "Konsekvens for samfundet - integritet", order = 21)
	private Double avgConsequenceIntegritySociety;
	@ExcelColumn(headerName = "Konsekvens for den registrerede - tilgængelighed", order = 16)
	private Double avgConsequenceAvailabilityRegistered;
	@ExcelColumn(headerName = "Konsekvens for organisationen - tilgængelighed", order = 19)
	private Double avgConsequenceAvailabilityOrganisation;
	@ExcelColumn(headerName = "Konsekvens for samfundet - tilgængelighed", order = 22)
	private Double avgConsequenceAvailabilitySociety;
	@ExcelColumn(headerName = "Konsekvens for samfundet - autenticitet", order = 23)
	private Double avgConsequenceAuthenticitySociety;
	@ExcelColumn(headerName = "Trusselstyper", order = 10)
	private String threatTypeList;
	@ExcelColumn(headerName = "Risikokataloger", order = 11)
	private String catalogList;
	@ExcludeFromExport
	private Double riskScore;
}

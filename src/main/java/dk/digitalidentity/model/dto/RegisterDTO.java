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

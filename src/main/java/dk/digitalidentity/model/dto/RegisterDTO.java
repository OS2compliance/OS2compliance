package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.ExcelColumn;
import dk.digitalidentity.model.ExcludeFromExport;
import dk.digitalidentity.model.entity.kle.KLEGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
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
}

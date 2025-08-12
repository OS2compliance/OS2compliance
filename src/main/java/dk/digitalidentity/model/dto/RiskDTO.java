package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.ExcelColumn;
import dk.digitalidentity.model.ExcludeFromExport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskDTO {
    private Long id;
	@ExcelColumn(headerName = "Titel", order = 1)
    private String name;
	@ExcelColumn(headerName = "Risikoejer", order = 4)
    private String responsibleUser;
	@ExcelColumn(headerName = "Fagområde", order = 3)
    private String responsibleOU;
	@ExcelColumn(headerName = "Type", order = 2)
    private String type;
	@ExcelColumn(headerName = "Dato", order = 7)
    private String date;
	@ExcelColumn(headerName = "Opgaver", order = 6)
    private Integer tasks;
	@ExcelColumn(headerName = "Risikovurdering", order = 8)
    private String assessment;
	@ExcludeFromExport
    private Integer assessmentOrder;
    private String threatAssessmentReportApprovalStatus;
	@ExcludeFromExport
    private boolean changeable;
	@ExcludeFromExport
    private boolean fromExternalSource;
	@ExcludeFromExport
    private String externalLink;
	@ExcelColumn(headerName = "Entitet", order = 5)
	private String relatedAssetsAndRegisters;
}

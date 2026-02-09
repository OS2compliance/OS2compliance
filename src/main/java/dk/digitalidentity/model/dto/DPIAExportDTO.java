package dk.digitalidentity.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class DPIAExportDTO {
	@ExcludeFromExport
	private Long id;

	@ExcelColumn(headerName = "Navn", order = 1)
	private String name;

	@ExcelColumn(headerName = "Ansvarlig bruger", order = 2)
	private String responsibleUserName;

	@ExcelColumn(headerName = "Ansvarlig afdeling", order = 3)
	private String responsibleOuName;

	@ExcelColumn(headerName = "Opdateringsdato", order = 4)
	@JsonFormat(pattern = "dd/MM-yyyy")
	private LocalDate userUpdatedDate;

	@ExcelColumn(headerName = "Antal opgaver", order = 5)
	private int taskCount;

	@ExcelColumn(headerName = "Status", order = 6)
	private String status;

	@ExcelColumn(headerName = "Screening konklusion", order = 7)
	private String screeningConclusion;

	@ExcelColumn(headerName = "Tags", order = 8)
	private List<TagDTO> tags;

	@ExcludeFromExport
	private Set<AllowedAction> allowedActions;
}
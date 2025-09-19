package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.ExcelColumn;
import dk.digitalidentity.model.ExcludeFromExport;
import dk.digitalidentity.model.dto.enums.AllowedAction;
import dk.digitalidentity.model.entity.enums.TaskResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    private Long id;
	@ExcelColumn(headerName = "Opgavenavn", order = 1)
    private String name;
	@ExcelColumn(headerName = "Opgave type", order = 2)
    private String taskType;
	@ExcelColumn(headerName = "Ansvarlig", order = 3)
    private String responsibleUser;
	@ExcelColumn(headerName = "Afdeling", order = 4)
    private String responsibleOU;
	@ExcelColumn(headerName = "Deadline", order = 6)
    private String nextDeadline;
	@ExcelColumn(headerName = "Gentages", order = 7)
    private String taskRepetition;
	@ExcludeFromExport
    private Integer taskRepetitionOrder;
	@ExcelColumn(headerName = "Status", order = 8)
    private boolean completed;
	@ExcludeFromExport
    private String taskResult;
	@ExcludeFromExport
    private Integer taskResultOrder;
	@ExcelColumn(headerName = "Tags", order = 5)
    private String tags;
	@ExcludeFromExport
    private Set<AllowedAction> allowedActions;
}

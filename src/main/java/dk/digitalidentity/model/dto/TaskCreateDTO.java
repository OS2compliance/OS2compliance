package dk.digitalidentity.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import dk.digitalidentity.model.entity.enums.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class TaskCreateDTO {

	private Long id;

	@NotBlank(message = "Task name is required")
	private String name;

	@NotNull(message = "Task type is required")
	private TaskType taskType;

	@JsonFormat(pattern = "dd/MM-yyyy")
	@NotNull(message = "Deadline is required")
	private LocalDate nextDeadline;

	private Set<String> responsibleUserUuids;
	private String responsibleOuUuid;
	private String departmentUuid;
	private String repetition;
	private String description;
	private boolean notifyResponsible = false;
	private boolean includeInReport = false;
	private List<Long> tagIds;
	private List<TaskLinkDTO> links;
	private Set<String> notificationReminders;
	private Long taskDescriptionTemplateId;
}

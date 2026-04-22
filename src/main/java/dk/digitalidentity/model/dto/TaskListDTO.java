package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.enums.RelationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskListDTO {
	private long id;
	private String title;
	private String responsibleUserName;
	private String responsibleOuName;
	private String taskType;
	private String deadline;
	private String repeats;
	private String status;
	private RelationType relationType;
}

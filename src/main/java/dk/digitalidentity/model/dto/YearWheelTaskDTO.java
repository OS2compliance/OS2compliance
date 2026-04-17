package dk.digitalidentity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YearWheelTaskDTO {
	private long id;
	private String name;
	private String taskType;
	private String repetition;
	private String deadline;
	private String responsibleNames;
	private String responsibleOU;
	private List<YearWheelTagDTO> tags;
	private String status; // "completed", "overdue", "upcoming"
}
package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.enums.TaskResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    private Long id;
    private String name;
    private String taskType;
    private String responsibleUser;
    private String responsibleOU;
    private String nextDeadline;
    private String taskRepetition;
    private Integer taskRepetitionOrder;
    private boolean completed;
    private TaskResult taskResult;
    private Integer taskResultOrder;
    private String tags;
    private boolean changeable;
}

package dk.digitalidentity.model.dto;

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
    private String name;
    private String taskType;
    private String responsibleUser;
    private String responsibleOU;
    private String nextDeadline;
    private String taskRepetition;
    private Integer taskRepetitionOrder;
    private boolean completed;
    private String taskResult;
    private Integer taskResultOrder;
    private String tags;
    private Set<AllowedAction> allowedActions;
}

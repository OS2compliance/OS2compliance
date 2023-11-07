package dk.digitalidentity.service.model;

import dk.digitalidentity.model.entity.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TaskDTO {
    private long id;
    private String name;
    private TaskType taskType;
    private String responsibleUser;
    private String deadline;
    private boolean passedDeadline;
}

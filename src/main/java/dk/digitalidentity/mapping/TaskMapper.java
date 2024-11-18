package dk.digitalidentity.mapping;

import dk.digitalidentity.model.dto.AssetDTO;
import dk.digitalidentity.model.dto.TaskDTO;
import dk.digitalidentity.model.entity.grid.AssetGrid;
import dk.digitalidentity.model.entity.grid.TaskGrid;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
import java.util.List;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;
import static dk.digitalidentity.util.NullSafe.nullSafe;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TaskMapper {

    @SuppressWarnings("Convert2MethodRef")
    default TaskDTO toDTO(final TaskGrid taskGrid) {
        return TaskDTO.builder()
                .id(taskGrid.getId())
                .name(taskGrid.getName())
                .responsibleUser(nullSafe(() -> taskGrid.getResponsibleUser().getName()))
                .responsibleOU(nullSafe(() -> taskGrid.getResponsibleOU().getName()))
                .nextDeadline(nullSafe(() -> taskGrid.getNextDeadline().format(DK_DATE_FORMATTER)))
                .taskRepetition(nullSafe(() -> taskGrid.getTaskRepetition().getMessage()))
                .taskRepetitionOrder(taskGrid.getTaskRepetitionOrder())
                .taskType(nullSafe(() -> taskGrid.getTaskType().getMessage()))
                .taskResult(nullSafe(() -> taskGrid.getTaskResult()))
                .taskResultOrder(taskGrid.getTaskResultOrder())
                .completed(nullSafe(taskGrid::isCompleted))
                .tags(nullSafe(() -> taskGrid.getTags()))
                .changeable(false)
                .build();
    }

    //provides a mapping that's set changeable to true if user is at least a superuser or uuid matches current user's uuid.
    default TaskDTO toDTO(final TaskGrid taskGrid, boolean superuser, String principalUuid) {
        TaskDTO taskDTO = toDTO(taskGrid);
        if(superuser || principalUuid.equals(taskGrid.getResponsibleUser().getUuid())) {
            taskDTO.setChangeable(true);
        }
        return taskDTO;
    }

    default List<TaskDTO> toDTO(List<TaskGrid> taskGrid) {
        List<TaskDTO> taskDTOS = new ArrayList<>();
        taskGrid.forEach(a -> taskDTOS.add(toDTO(a)));
        return taskDTOS;
    }

    //provides a list of mapping that's set changeable to true if user is at least a superuser or uuid matches current user's uuid.
    default List<TaskDTO> toDTO(List<TaskGrid> taskGrid, boolean superuser, String principalUuid) {
        List<TaskDTO> taskDTOS = new ArrayList<>();
        taskGrid.forEach(a -> taskDTOS.add(toDTO(a, superuser, principalUuid)));
        return taskDTOS;
    }

}

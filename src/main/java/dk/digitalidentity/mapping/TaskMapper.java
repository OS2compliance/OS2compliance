package dk.digitalidentity.mapping;

import dk.digitalidentity.model.dto.TaskDTO;
import dk.digitalidentity.model.entity.grid.TaskGrid;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

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
                .build();
    }

    List<TaskDTO> toDTO(List<TaskGrid> taskGrids);

}

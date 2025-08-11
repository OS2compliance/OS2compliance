package dk.digitalidentity.mapping;

import dk.digitalidentity.model.dto.AssetDTO;
import dk.digitalidentity.model.dto.TaskDTO;
import dk.digitalidentity.model.entity.grid.AssetGrid;
import dk.digitalidentity.model.entity.grid.TaskGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.AssetService;
import lombok.RequiredArgsConstructor;
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
        TaskDTO taskDTO = TaskDTO.builder()
                .id(taskGrid.getId())
                .name(taskGrid.getName())
                .responsibleUser(nullSafe(() -> taskGrid.getResponsibleUser().getName()))
                .responsibleOU(nullSafe(() -> taskGrid.getResponsibleOU().getName()))
                .nextDeadline(nullSafe(() -> taskGrid.getNextDeadline().format(DK_DATE_FORMATTER)))
                .taskRepetition(nullSafe(() -> taskGrid.getTaskRepetition().getMessage()))
                .taskRepetitionOrder(taskGrid.getTaskRepetitionOrder())
                .taskType(nullSafe(() -> taskGrid.getTaskType().getMessage()))
                .taskResult(nullSafe(() -> taskGrid.getTaskResult().getValue()))
                .taskResultOrder(taskGrid.getTaskResultOrder())
                .completed(nullSafe(taskGrid::isCompleted))
                .tags(nullSafe(() -> taskGrid.getTags()))
                .changeable(false)
                .build();

		taskDTO.setChangeable(SecurityUtil.isOperationAllowed(Roles.UPDATE_ALL) || SecurityUtil.getPrincipalUuid().equals(taskGrid.getResponsibleUser().getUuid()));
		return taskDTO;
    }

    default List<TaskDTO> toDTO(List<TaskGrid> taskGrid) {
        List<TaskDTO> taskDTOS = new ArrayList<>();
        taskGrid.forEach(a -> taskDTOS.add(toDTO(a)));
        return taskDTOS;
    }
}

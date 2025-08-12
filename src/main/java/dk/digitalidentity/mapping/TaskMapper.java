package dk.digitalidentity.mapping;

import dk.digitalidentity.model.dto.AssetDTO;
import dk.digitalidentity.model.dto.TaskDTO;
import dk.digitalidentity.model.dto.enums.AllowedAction;
import dk.digitalidentity.model.entity.grid.AssetGrid;
import dk.digitalidentity.model.entity.grid.TaskGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
                .build();

		Set<AllowedAction> allowedActions = new HashSet<>();
		boolean isResponsible =	(taskGrid.getResponsibleUser() != null && taskGrid.getResponsibleUser().getUuid().equals(SecurityUtil.getPrincipalUuid()));
		if (SecurityUtil.isOperationAllowed(Roles.UPDATE_ALL)
				|| (isResponsible && SecurityUtil.isOperationAllowed(Roles.UPDATE_OWNER_ONLY))) {
			allowedActions.add(AllowedAction.UPDATE);
		}
		if (SecurityUtil.isOperationAllowed(Roles.DELETE_ALL)
				|| (isResponsible && SecurityUtil.isOperationAllowed(Roles.DELETE_OWNER_ONLY))) {
			allowedActions.add(AllowedAction.DELETE);
		}

		taskDTO.setAllowedActions(allowedActions);

		return taskDTO;
    }

    default List<TaskDTO> toDTO(List<TaskGrid> taskGrid) {
        List<TaskDTO> taskDTOS = new ArrayList<>();
        taskGrid.forEach(a -> taskDTOS.add(toDTO(a)));
        return taskDTOS;
    }
}

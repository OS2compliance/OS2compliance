package dk.digitalidentity.mapping;

import dk.digitalidentity.model.dto.TagDTO;
import dk.digitalidentity.model.dto.TaskDTO;
import dk.digitalidentity.model.dto.enums.AllowedAction;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.grid.TaskGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;
import static dk.digitalidentity.util.NullSafe.nullSafe;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TaskMapper {

	@SuppressWarnings("Convert2MethodRef")
	default TaskDTO toDTO(final TaskGrid taskGrid, Map<Long, Tag> tagsById) {
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
				.tags(nullSafe(() -> Arrays.stream(taskGrid.getTagIds().split(",")).map(sid -> {
									if (sid.trim().isEmpty()) {
										return null;
									}
									Long id = Long.parseLong(sid);
									Tag tag = tagsById.getOrDefault(id, null);
									return TagDTO.builder()
											.label(tag.getValue())
											.color(tag.getColor().getHexCode())
											.contrast(tag.getColor().getContrastHexCode())
											.build();
								}).filter(Objects::nonNull)
								.collect(Collectors.toSet())
				))
				.lastCompletionDate(nullSafe(() -> taskGrid.getLastCompletionDate().format(DK_DATE_FORMATTER)))
				.build();

		Set<AllowedAction> allowedActions = new HashSet<>();
		boolean isResponsible = (taskGrid.getResponsibleUser() != null && taskGrid.getResponsibleUser().getUuid().equals(SecurityUtil.getPrincipalUuid()));
		if (SecurityUtil.isOperationAllowed(Roles.UPDATE_ALL)
				|| (isResponsible && SecurityUtil.isOperationAllowed(Roles.UPDATE_OWNER_ONLY))) {
			allowedActions.add(AllowedAction.UPDATE);
		}
		if (SecurityUtil.isOperationAllowed(Roles.UPDATE_ALL)
				|| (isResponsible && SecurityUtil.isOperationAllowed(Roles.CREATE_ALL))) {
			allowedActions.add(AllowedAction.COPY);
		}
		if (SecurityUtil.isOperationAllowed(Roles.DELETE_ALL)
				|| (isResponsible && SecurityUtil.isOperationAllowed(Roles.DELETE_OWNER_ONLY))) {
			allowedActions.add(AllowedAction.DELETE);
		}

		taskDTO.setAllowedActions(allowedActions);

		return taskDTO;
	}

	default List<TaskDTO> toDTO(List<TaskGrid> taskGrid, Map<Long, Tag> tagsById) {
		List<TaskDTO> taskDTOS = new ArrayList<>();
		taskGrid.forEach(a -> taskDTOS.add(toDTO(a, tagsById)));
		return taskDTOS;
	}
}

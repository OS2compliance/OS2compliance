package dk.digitalidentity.mapping;

import dk.digitalidentity.model.dto.TagDTO;
import dk.digitalidentity.model.dto.TaskCreateDTO;
import dk.digitalidentity.model.dto.TaskDTO;
import dk.digitalidentity.model.dto.enums.AllowedAction;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.grid.TaskGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.OrganisationService;
import dk.digitalidentity.service.UserService;
import dk.digitalidentity.service.tag.TagService;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;
import static dk.digitalidentity.util.NullSafe.nullSafe;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TaskMapper {

	@SuppressWarnings("Convert2MethodRef")
	default TaskDTO toDTO(final TaskGrid taskGrid, Map<Long, Tag> tagsById) {
		List<TagDTO> tags = TagService.toTagDTO(taskGrid.getTagIds(), tagsById).stream().sorted(Comparator.comparing(TagDTO::getLabel)).toList();

		TaskDTO taskDTO = TaskDTO.builder()
				.id(taskGrid.getId())
				.name(taskGrid.getName())
				.responsibleUser(nullSafe(() -> taskGrid.getResponsibleNames()))
				.responsibleOU(nullSafe(() -> taskGrid.getResponsibleOU().getName()))
				.nextDeadline(nullSafe(() -> taskGrid.getNextDeadline().format(DK_DATE_FORMATTER)))
				.taskRepetition(nullSafe(() -> taskGrid.getTaskRepetition().getMessage()))
				.taskRepetitionOrder(taskGrid.getTaskRepetitionOrder())
				.taskType(nullSafe(() -> taskGrid.getTaskType().getMessage()))
				.taskResult(nullSafe(() -> taskGrid.getTaskResult().getValue()))
				.taskResultOrder(taskGrid.getTaskResultOrder())
				.completed(nullSafe(taskGrid::isCompleted))
				.tags(tags)
				.lastCompletionDate(nullSafe(() -> taskGrid.getLastCompletionDate().format(DK_DATE_FORMATTER)))
				.build();

		Set<AllowedAction> allowedActions = new HashSet<>();
		boolean isResponsible = (taskGrid.getResponsibleUserUuids() != null &&
				taskGrid.getResponsibleUserUuidsAsSet().contains(SecurityUtil.getPrincipalUuid()));
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

	@Mapping(target = "id", source = "dto.id")
	@Mapping(target = "name", source = "dto.name")
	@Mapping(target = "taskType", source = "dto.taskType")
	@Mapping(target = "nextDeadline", source = "dto.nextDeadline")
	@Mapping(target = "repetition", expression = "java(mapRepetition(dto.getRepetition()))")
	@Mapping(target = "description", source = "dto.description")
	@Mapping(target = "notifyResponsible", source = "dto.notifyResponsible")
	@Mapping(target = "includeInReport", source = "dto.includeInReport")
	@Mapping(target = "responsibleUser", source = "responsibleUser")
	@Mapping(target = "responsibleOu", source = "responsibleOu")
	@Mapping(target = "department", source = "department")
	@Mapping(target = "tags", source = "tags")
	@Mapping(target = "links", ignore = true)
	// Ignore fields we dont need
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "relationType", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "createdBy", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "updatedBy", ignore = true)
	@Mapping(target = "deleted", ignore = true)
	@Mapping(target = "localizedEnums", ignore = true)
	@Mapping(target = "properties", ignore = true)
	@Mapping(target = "logs", ignore = true)
	@Mapping(target = "status", ignore = true)
	Task toEntity(TaskCreateDTO dto, User responsibleUser, OrganisationUnit responsibleOu, OrganisationUnit department, Set<Tag> tags);

	default TaskRepetition mapRepetition(String repetition) {
		if (repetition == null || repetition.trim().isEmpty()) {
			return null;
		}
		try {
			return TaskRepetition.valueOf(repetition.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid repetition value: " + repetition);
		}
	}
}

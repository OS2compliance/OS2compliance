package dk.digitalidentity.mapping;

import dk.digitalidentity.model.dto.RelatedEntityDTO;
import dk.digitalidentity.model.dto.TagDTO;
import dk.digitalidentity.model.dto.TaskCreateDTO;
import dk.digitalidentity.model.dto.TaskDTO;
import dk.digitalidentity.model.dto.enums.AllowedAction;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.NotificationSetting;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.grid.TaskGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.tag.TagService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
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
				.responsibleNames(nullSafe(() -> taskGrid.getResponsibleNames()))
				.responsibleOU(nullSafe(() -> taskGrid.getResponsibleOU().getName()))
				.nextDeadline(nullSafe(() -> taskGrid.getNextDeadline().format(DK_DATE_FORMATTER)))
				.taskRepetition(nullSafe(() -> taskGrid.getTaskRepetition().getMessage()))
				.taskRepetitionOrder(taskGrid.getTaskRepetitionOrder())
				.taskType(nullSafe(() -> taskGrid.getTaskType().getMessage()))
				.taskResult(nullSafe(() -> taskGrid.getTaskResult()))
				.taskResultOrder(taskGrid.getTaskResultOrder())
				.completed(nullSafe(taskGrid::isCompleted))
				.tags(tags)
				.lastCompletionDate(nullSafe(() -> taskGrid.getLastCompletionDate().format(DK_DATE_FORMATTER)))
				.relatedEntities(taskGrid.getRelatedEntitiesDTO().stream().map(RelatedEntityDTO::toLink).toList())
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
	@Mapping(target = "responsibleUsers", source = "responsibleUsers")
	@Mapping(target = "responsibleOu", source = "responsibleOu")
	@Mapping(target = "department", source = "department")
	@Mapping(target = "tags", source = "tags")
	@Mapping(target = "taskDescriptionTemplate", source = "taskDescriptionTemplate")
	@Mapping(target = "notificationReminders", expression = "java(mapNotificationReminders(dto.getNotificationReminders()))")
	@Mapping(target = "links", ignore = true)
	@Mapping(target = "subTasks", ignore = true)
	// Ignore fields we dont need
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "preservedResponsibleUserUuids", ignore = true)
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
	Task toEntity(TaskCreateDTO dto, Set<User> responsibleUsers, OrganisationUnit responsibleOu, OrganisationUnit department, Set<Tag> tags, ChoiceValue taskDescriptionTemplate);

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

	default Set<NotificationSetting> mapNotificationReminders(Set<String> notificationReminders) {
		if (notificationReminders == null || notificationReminders.isEmpty()) {
			return new HashSet<>();
		}

		return notificationReminders.stream()
				.filter(reminder -> reminder != null && !reminder.trim().isEmpty())
				.map(reminder -> {
					try {
						return NotificationSetting.valueOf(reminder.toUpperCase());
					} catch (IllegalArgumentException e) {
						throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid notification reminder value: " + reminder);
					}
				})
				.collect(Collectors.toSet());
	}

	default TaskDTO toDTOForExport(final Task task, Map<Long, Tag> tagsById) {
		// Map tags from tagsById map
		List<TagDTO> tags = task.getTags().stream()
				.map(tag -> {
					Tag loadedTag = tagsById.get(tag.getId());
					if (loadedTag == null) {
						return null;
					}
					return TagDTO.builder()
							.label(loadedTag.getValue())
							.color(loadedTag.getColor().getHexCode())
							.contrast(loadedTag.getColor().getContrastHexCode())
							.build();
				})
				.filter(java.util.Objects::nonNull)
				.sorted(Comparator.comparing(TagDTO::getLabel))
				.toList();

		// Get responsible user names
		String responsibleNames = task.getResponsibleUsers().stream()
				.map(User::getName)
				.collect(Collectors.joining(", "));

		// Check if task is completed
		boolean completed = task.getTaskType() == TaskType.TASK && !task.getLogs().isEmpty();

		return TaskDTO.builder()
				.id(task.getId())
				.name(task.getName())
				.taskType(nullSafe(() -> task.getTaskType().getMessage()))
				.responsibleNames(responsibleNames)
				.responsibleOU(nullSafe(() -> task.getResponsibleOu().getName()))
				.nextDeadline(nullSafe(() -> task.getNextDeadline().format(DK_DATE_FORMATTER)))
				.taskRepetition(nullSafe(() -> task.getRepetition().getMessage()))
				.completed(completed)
				.tags(tags)
				.build();
	}

	default List<TaskDTO> toDTOForExportFromTasks(List<Task> tasks, Map<Long, Tag> tagsById) {
		List<TaskDTO> taskDTOS = new ArrayList<>();
		tasks.forEach(task -> taskDTOS.add(toDTOForExport(task, tagsById)));
		return taskDTOS;
	}
}

package dk.digitalidentity.service;

import dk.digitalidentity.model.dto.YearWheelDTO;
import dk.digitalidentity.model.dto.YearWheelTagDTO;
import dk.digitalidentity.model.dto.YearWheelTaskDTO;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.grid.TaskGrid;
import dk.digitalidentity.service.tag.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class YearWheelService {
	private static final DateTimeFormatter DK_FORMAT = DateTimeFormatter.ofPattern("dd/MM-yyyy");
	private final TaskService taskService;
	private final TagService tagService;

	/**
	 * Builds the year wheel data for a given year, respecting user access and filters.
	 */
	public YearWheelDTO buildYearWheel(int year, Map<String, String> filters, User user, boolean onlyMine) {
		Page<TaskGrid> gridPage = taskService.getTasks(null, "ASC", filters, 0, Integer.MAX_VALUE, user, onlyMine);

		List<TaskGrid> tasks = gridPage.getContent().stream()
				.filter(TaskGrid::isIncludeInReport)
				.toList();

		// Load year-wheel tags once
		Map<Long, Tag> allTagsById = tagService.findAll().stream()
				.collect(Collectors.toMap(Tag::getId, t -> t, (a, b) -> b));

		Map<Integer, List<YearWheelTaskDTO>> months = new LinkedHashMap<>();
		for (int m = 1; m <= 12; m++) {
			months.put(m, new ArrayList<>());
		}

		Set<Tag> usedYearWheelTags = new TreeSet<>(Comparator.comparing(Tag::getId));

		for (TaskGrid task : tasks) {
			LocalDate deadline = task.getNextDeadline().toLocalDate();
			LocalDate createdAt = task.getCreatedAt() != null
					? task.getCreatedAt().toLocalDate()
					: null;
			List<Integer> occurrenceMonths = calculateOccurrenceMonths(year, deadline, task.getTaskRepetition(), createdAt);

			// Resolve year-wheel tags from tagIds
			List<Tag> yearWheelTags = resolveYearWheelTags(task.getTagIds(), allTagsById);
			List<YearWheelTagDTO> tagDTOs = yearWheelTags.stream()
					.map(this::toTagDTO)
					.toList();
			usedYearWheelTags.addAll(yearWheelTags);

			String responsibleOU = task.getResponsibleOU() != null
					? task.getResponsibleOU().getName()
					: "";

			String repetitionLabel = task.getTaskRepetition() != null
					? task.getTaskRepetition().getMessage()
					: "Ingen";

			for (int month : occurrenceMonths) {
				LocalDate occurrenceDeadline = calculateOccurrenceDeadline(year, month, deadline);
				String status = calculateOccurrenceStatus(task, occurrenceDeadline);

				YearWheelTaskDTO dto = YearWheelTaskDTO.builder()
						.id(task.getId())
						.name(task.getName())
						.taskType(task.getTaskType().getMessage())
						.repetition(repetitionLabel)
						.deadline(occurrenceDeadline.format(DK_FORMAT))
						.responsibleNames(task.getResponsibleNames() != null ? task.getResponsibleNames() : "")
						.responsibleOU(responsibleOU)
						.tags(tagDTOs)
						.status(status)
						.build();

				months.get(month).add(dto);
			}
		}

		months.values().forEach(list ->
				list.sort(Comparator.comparing(YearWheelTaskDTO::getDeadline)));

		List<YearWheelTagDTO> legendTags = usedYearWheelTags.stream()
				.map(this::toTagDTO)
				.toList();

		return YearWheelDTO.builder()
				.year(year)
				.tags(legendTags)
				.months(months)
				.build();
	}

	private String calculateOccurrenceStatus(TaskGrid task, LocalDate occurrenceDeadline) {
		// TASK type: completed if any log exists
		if (task.isCompleted()) {
			return "completed";
		}

		// CHECK type: compare occurrence deadline with last completion
		LocalDate lastCompletion = task.getLastCompletionDate();
		if (lastCompletion != null && !occurrenceDeadline.isAfter(lastCompletion)) {
			return "completed";
		}

		// Not completed — check if overdue
		if (occurrenceDeadline.isBefore(LocalDate.now())) {
			return "overdue";
		}

		return "upcoming";
	}

	private List<Tag> resolveYearWheelTags(String tagIds, Map<Long, Tag> allTagsById) {
		if (tagIds == null || tagIds.isBlank()) {
			return List.of();
		}
		return Arrays.stream(tagIds.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.map(Long::parseLong)
				.map(allTagsById::get)
				.filter(Objects::nonNull)
				.filter(Tag::isYearWheel)
				.toList();
	}

	/**
	 * Calculates which months in the target year a task occurs,
	 * based on its nextDeadline and repetition pattern.
	 */
	List<Integer> calculateOccurrenceMonths(int targetYear, LocalDate nextDeadline, TaskRepetition repetition, LocalDate createdAt) {
		List<Integer> months = new ArrayList<>();

		if (nextDeadline == null) {
			return months;
		}

		// Earliest date this task can appear
		LocalDate earliestDate = createdAt != null ? createdAt : nextDeadline;

		// No repetition: only include if deadline falls in target year and after creation
		if (repetition == null || repetition == TaskRepetition.NONE) {
			if (nextDeadline.getYear() == targetYear && !nextDeadline.isBefore(earliestDate)) {
				months.add(nextDeadline.getMonthValue());
			}
			return months;
		}

		int intervalMonths = getIntervalMonths(repetition);
		if (intervalMonths <= 0) {
			if (nextDeadline.getYear() == targetYear && !nextDeadline.isBefore(earliestDate)) {
				months.add(nextDeadline.getMonthValue());
			}
			return months;
		}

		LocalDate yearStart = LocalDate.of(targetYear, 1, 1);
		LocalDate yearEnd = LocalDate.of(targetYear, 12, 31);

		// Effective start: the later of yearStart and earliestDate
		LocalDate effectiveStart = yearStart.isBefore(earliestDate) ? earliestDate : yearStart;

		// If the effective start is after yearEnd, no occurrences this year
		if (effectiveStart.isAfter(yearEnd)) {
			return months;
		}

		// Step backwards from nextDeadline to find the earliest occurrence at or after effectiveStart
		LocalDate cursor = nextDeadline;
		while (cursor.minusMonths(intervalMonths).isAfter(effectiveStart)
				|| cursor.minusMonths(intervalMonths).isEqual(effectiveStart)) {
			cursor = cursor.minusMonths(intervalMonths);
		}

		// If we overshot past effectiveStart, step forward
		if (cursor.isBefore(effectiveStart)) {
			cursor = cursor.plusMonths(intervalMonths);
		}

		// Collect all occurrences within the target year
		while (!cursor.isAfter(yearEnd)) {
			if (!cursor.isBefore(effectiveStart)) {
				int month = cursor.getMonthValue();
				if (!months.contains(month)) {
					months.add(month);
				}
			}
			cursor = cursor.plusMonths(intervalMonths);
		}

		return months;
	}

	/**
	 * Returns the interval in months for a given repetition type.
	 */
	private int getIntervalMonths(TaskRepetition repetition) {
		return switch (repetition) {
			case MONTHLY -> 1;
			case EVERY_2_MONTHS -> 2;
			case QUARTERLY, EVERY_3_MONTHS -> 3;
			case EVERY_4_MONTHS -> 4;
			case HALF_YEARLY -> 6;
			case YEARLY -> 12;
			case EVERY_SECOND_YEAR -> 24;
			case EVERY_THIRD_YEAR -> 36;
			default -> 0;
		};
	}

	/**
	 * Calculates the specific deadline date for an occurrence in a given month.
	 * Uses the original day-of-month from nextDeadline, clamped to the month's last day.
	 */
	private LocalDate calculateOccurrenceDeadline(int year, int month, LocalDate originalDeadline) {
		int dayOfMonth = originalDeadline.getDayOfMonth();
		int maxDay = LocalDate.of(year, month, 1).lengthOfMonth();
		int actualDay = Math.min(dayOfMonth, maxDay);
		return LocalDate.of(year, month, actualDay);
	}

	private YearWheelTagDTO toTagDTO(Tag tag) {
		return YearWheelTagDTO.builder()
				.id(tag.getId())
				.value(tag.getValue())
				.color(tag.getColor().getHexCode())
				.contrastColor(tag.getColor().getContrastHexCode())
				.build();
	}
}
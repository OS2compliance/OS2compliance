package dk.digitalidentity.service;

import dk.digitalidentity.model.dto.YearWheelDTO;
import dk.digitalidentity.model.dto.YearWheelTagDTO;
import dk.digitalidentity.model.dto.YearWheelTaskDTO;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
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

		Map<Long, Set<LocalDate>> loggedDeadlines = taskService.getLoggedDeadlines(tasks.stream()
				.map(TaskGrid::getId)
				.toList());

		Map<Integer, List<YearWheelTaskDTO>> months = new LinkedHashMap<>();
		for (int m = 1; m <= 12; m++) {
			months.put(m, new ArrayList<>());
		}

		Set<Tag> usedYearWheelTags = new TreeSet<>(Comparator.comparing(Tag::getId));

		for (TaskGrid task : tasks) {
			if (task.getNextDeadline() == null) {
				// tasks.next_deadline is nullable and the grid view passes the null through - a single
				// such task must not take the whole year wheel down
				continue;
			}
			LocalDate deadline = task.getNextDeadline().toLocalDate();
			List<LocalDate> closed = closedDeadlines(year, deadline, loggedDeadlines.getOrDefault(task.getId(), Set.of()));
			List<LocalDate> open = openDeadlines(year, deadline, task.getTaskRepetition());

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

			for (LocalDate occurrence : closed) {
				months.get(occurrence.getMonthValue()).add(toOccurrenceDTO(task, occurrence, "completed", repetitionLabel, responsibleOU, tagDTOs));
			}
			for (LocalDate occurrence : open) {
				months.get(occurrence.getMonthValue()).add(toOccurrenceDTO(task, occurrence, calculateOccurrenceStatus(task, occurrence), repetitionLabel, responsibleOU, tagDTOs));
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

	private YearWheelTaskDTO toOccurrenceDTO(TaskGrid task, LocalDate occurrence, String status, String repetitionLabel,
			String responsibleOU, List<YearWheelTagDTO> tagDTOs) {
		return YearWheelTaskDTO.builder()
				.id(task.getId())
				.name(task.getName())
				.taskType(task.getTaskType().getMessage())
				.repetition(repetitionLabel)
				.deadline(occurrence.format(DK_FORMAT))
				.responsibleNames(task.getResponsibleNames() != null ? task.getResponsibleNames() : "")
				.responsibleOU(responsibleOU)
				.tags(tagDTOs)
				.status(status)
				.build();
	}

	/**
	 * Past occurrences come from the logs: a completion that moved the deadline on closed the deadline it was
	 * logged against. A completion that left the deadline where it was (a second one in the same period) did not.
	 */
	List<LocalDate> closedDeadlines(int targetYear, LocalDate nextDeadline, Set<LocalDate> loggedDeadlines) {
		return loggedDeadlines.stream()
				.filter(d -> d.getYear() == targetYear && d.isBefore(nextDeadline))
				.sorted()
				.toList();
	}

	/** Open occurrences start at the next deadline and repeat forward from it. */
	List<LocalDate> openDeadlines(int targetYear, LocalDate nextDeadline, TaskRepetition repetition) {
		int intervalMonths = repetition == null ? 0 : getIntervalMonths(repetition);
		List<LocalDate> deadlines = new ArrayList<>();
		LocalDate cursor = nextDeadline;
		for (int i = 1; cursor.getYear() <= targetYear; i++) {
			if (cursor.getYear() == targetYear) {
				deadlines.add(cursor);
			}
			if (intervalMonths <= 0) {
				break;
			}
			// stepped from nextDeadline each time, so a deadline on the 31st is not dragged to the 28th for good
			cursor = nextDeadline.plusMonths((long) intervalMonths * i);
		}
		return deadlines;
	}

	private String calculateOccurrenceStatus(TaskGrid task, LocalDate occurrenceDeadline) {
		if (task.isCompleted()) {
			return "completed";
		}
		if (task.isInProgress() && occurrenceDeadline.isEqual(task.getNextDeadline().toLocalDate())) {
			return "inprogress";
		}
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

	private YearWheelTagDTO toTagDTO(Tag tag) {
		return YearWheelTagDTO.builder()
				.id(tag.getId())
				.value(tag.getValue())
				.color(tag.getColor().getHexCode())
				.contrastColor(tag.getColor().getContrastHexCode())
				.build();
	}
}

package dk.digitalidentity.service;

import dk.digitalidentity.model.dto.YearWheelDTO;
import dk.digitalidentity.model.dto.YearWheelTaskDTO;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.grid.TaskGrid;
import dk.digitalidentity.service.tag.TagService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class YearWheelServiceTest {
	private final TaskService taskService = mock(TaskService.class);
	private final TagService tagService = mock(TagService.class);
	private final YearWheelService yearWheelService = new YearWheelService(taskService, tagService);

	@Test
	public void openOccurrencesStartAtTheNextDeadline() {
		TaskGrid task = reportableTask(7L, LocalDate.of(2026, 10, 30), TaskRepetition.MONTHLY);
		task.setStartDate(LocalDate.of(2026, 9, 30));
		stubTasks(task, Map.of());

		assertThat(occurrences(yearWheelService.buildYearWheel(2026, Map.of(), null, false)))
				.extracting(YearWheelTaskDTO::getDeadline)
				.containsExactly("30/10-2026", "30/11-2026", "30/12-2026");
	}

	@Test
	public void periodClosedByAnEarlyCompletionShowsAsCompleted() {
		// Due 15/09-2026, completed 01/09-2026, so the deadline moved on to 15/09-2027
		TaskGrid task = reportableTask(7L, LocalDate.of(2027, 9, 15), TaskRepetition.YEARLY);
		stubTasks(task, Map.of(7L, Set.of(LocalDate.of(2026, 9, 15))));

		assertThat(occurrences(yearWheelService.buildYearWheel(2026, Map.of(), null, false)))
				.extracting(YearWheelTaskDTO::getDeadline, YearWheelTaskDTO::getStatus)
				.containsExactly(tuple("15/09-2026", "completed"));
	}

	@Test
	public void secondCompletionInTheSamePeriodLeavesTheNextDeadlineOpen() {
		// The second completion was logged against 01/12-2027 but did not move it
		TaskGrid task = reportableTask(7L, LocalDate.of(2027, 12, 1), TaskRepetition.YEARLY);
		stubTasks(task, Map.of(7L, Set.of(LocalDate.of(2026, 12, 1), LocalDate.of(2027, 12, 1))));

		assertThat(occurrences(yearWheelService.buildYearWheel(2027, Map.of(), null, false)))
				.extracting(YearWheelTaskDTO::getDeadline, YearWheelTaskDTO::getStatus)
				.containsExactly(tuple("01/12-2027", "upcoming"));
	}

	@Test
	public void periodSkippedByALateCompletionIsLeftOut() {
		// Due 01/01-2025, completed 01/03-2026, so the deadline jumped to 01/01-2027
		TaskGrid task = reportableTask(7L, LocalDate.of(2027, 1, 1), TaskRepetition.YEARLY);
		stubTasks(task, Map.of(7L, Set.of(LocalDate.of(2025, 1, 1))));

		assertThat(occurrences(yearWheelService.buildYearWheel(2026, Map.of(), null, false))).isEmpty();
	}

	@Test
	public void everySecondYearSkipsTheYearsInBetween() {
		LocalDate nextDeadline = LocalDate.of(2026, 3, 4);

		assertThat(yearWheelService.openDeadlines(2027, nextDeadline, TaskRepetition.EVERY_SECOND_YEAR)).isEmpty();
		assertThat(yearWheelService.openDeadlines(2028, nextDeadline, TaskRepetition.EVERY_SECOND_YEAR))
				.containsExactly(LocalDate.of(2028, 3, 4));
	}

	@Test
	public void taskWithoutRepetitionOnlyAppearsInItsDeadlineYear() {
		LocalDate deadline = LocalDate.of(2027, 8, 12);

		assertThat(yearWheelService.openDeadlines(2027, deadline, TaskRepetition.NONE)).containsExactly(deadline);
		assertThat(yearWheelService.openDeadlines(2028, deadline, TaskRepetition.NONE)).isEmpty();
	}

	@Test
	public void deadlineOnTheLastDayOfAMonthIsNotDraggedToTheShortestMonth() {
		assertThat(yearWheelService.openDeadlines(2026, LocalDate.of(2026, 10, 31), TaskRepetition.MONTHLY))
				.containsExactly(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 31));
	}

	@Test
	public void wheelSkipsTasksWithoutADeadline() {
		TaskGrid task = reportableTask(7L, null, TaskRepetition.YEARLY);
		stubTasks(task, Map.of());

		assertThat(occurrences(yearWheelService.buildYearWheel(2026, Map.of(), null, false))).isEmpty();
	}

	private TaskGrid reportableTask(final Long id, final LocalDate nextDeadline, final TaskRepetition repetition) {
		TaskGrid task = new TaskGrid();
		task.setId(id);
		task.setName("Årlig kontrol");
		task.setTaskType(TaskType.CHECK);
		task.setTaskRepetition(repetition);
		task.setNextDeadline(nextDeadline != null ? nextDeadline.atStartOfDay() : null);
		task.setIncludeInReport(true);
		return task;
	}

	private void stubTasks(final TaskGrid task, final Map<Long, Set<LocalDate>> loggedDeadlines) {
		Page<TaskGrid> page = new PageImpl<>(List.of(task));
		doReturn(page).when(taskService).getTasks(any(), any(), any(), anyInt(), anyInt(), any(), anyBoolean());
		doReturn(List.of()).when(tagService).findAll();
		doReturn(loggedDeadlines).when(taskService).getLoggedDeadlines(any());
	}

	private List<YearWheelTaskDTO> occurrences(final YearWheelDTO wheel) {
		return wheel.getMonths().values().stream().flatMap(List::stream).toList();
	}
}

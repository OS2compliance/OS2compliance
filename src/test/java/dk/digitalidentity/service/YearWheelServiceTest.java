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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the occurrence projection behind the year wheel. The interesting cases are all about the
 * lower bound: the wheel projects a repeating task backwards from its next deadline, and must not project
 * past the first deadline the task ever had.
 */
public class YearWheelServiceTest {
	private final TaskService taskService = mock(TaskService.class);
	private final TagService tagService = mock(TagService.class);
	private final YearWheelService yearWheelService = new YearWheelService(taskService, tagService);

	@Test
	public void yearlyTaskDoesNotAppearBeforeItsFirstDeadline() {
		// Created in 2026 but first due 12/08-2027, so 2026 holds no occurrence at all
		LocalDate firstDeadline = LocalDate.of(2027, 8, 12);

		assertThat(yearWheelService.calculateOccurrenceMonths(2026, firstDeadline, TaskRepetition.YEARLY, firstDeadline))
				.isEmpty();
		assertThat(yearWheelService.calculateOccurrenceMonths(2027, firstDeadline, TaskRepetition.YEARLY, firstDeadline))
				.containsExactly(8);
	}

	@Test
	public void yearlyTaskAppearsInEveryYearAfterItsFirstDeadline() {
		LocalDate firstDeadline = LocalDate.of(2027, 8, 12);

		assertThat(yearWheelService.calculateOccurrenceMonths(2028, firstDeadline, TaskRepetition.YEARLY, firstDeadline))
				.containsExactly(8);
	}

	@Test
	public void completedTaskKeepsItsHistoricOccurrences() {
		// Due 12/08 every year since 2024, completed twice, so the deadline has moved on to 2026
		LocalDate nextDeadline = LocalDate.of(2026, 8, 12);
		LocalDate firstDeadline = LocalDate.of(2024, 8, 12);

		assertThat(yearWheelService.calculateOccurrenceMonths(2024, nextDeadline, TaskRepetition.YEARLY, firstDeadline))
				.containsExactly(8);
		assertThat(yearWheelService.calculateOccurrenceMonths(2025, nextDeadline, TaskRepetition.YEARLY, firstDeadline))
				.containsExactly(8);
		assertThat(yearWheelService.calculateOccurrenceMonths(2023, nextDeadline, TaskRepetition.YEARLY, firstDeadline))
				.isEmpty();
	}

	@Test
	public void monthlyTaskStartsInTheMonthOfItsFirstDeadline() {
		LocalDate firstDeadline = LocalDate.of(2026, 8, 12);

		assertThat(yearWheelService.calculateOccurrenceMonths(2026, firstDeadline, TaskRepetition.MONTHLY, firstDeadline))
				.containsExactly(8, 9, 10, 11, 12);
		assertThat(yearWheelService.calculateOccurrenceMonths(2027, firstDeadline, TaskRepetition.MONTHLY, firstDeadline))
				.containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
	}

	@Test
	public void quarterlyTaskFollowsItsIntervalFromTheFirstDeadline() {
		LocalDate firstDeadline = LocalDate.of(2026, 5, 1);

		assertThat(yearWheelService.calculateOccurrenceMonths(2026, firstDeadline, TaskRepetition.QUARTERLY, firstDeadline))
				.containsExactly(5, 8, 11);
	}

	@Test
	public void everySecondYearSkipsTheYearsInBetween() {
		LocalDate firstDeadline = LocalDate.of(2026, 3, 4);

		assertThat(yearWheelService.calculateOccurrenceMonths(2026, firstDeadline, TaskRepetition.EVERY_SECOND_YEAR, firstDeadline))
				.containsExactly(3);
		assertThat(yearWheelService.calculateOccurrenceMonths(2027, firstDeadline, TaskRepetition.EVERY_SECOND_YEAR, firstDeadline))
				.isEmpty();
		assertThat(yearWheelService.calculateOccurrenceMonths(2028, firstDeadline, TaskRepetition.EVERY_SECOND_YEAR, firstDeadline))
				.containsExactly(3);
	}

	@Test
	public void taskWithoutRepetitionOnlyAppearsInItsDeadlineYear() {
		LocalDate deadline = LocalDate.of(2027, 8, 12);

		assertThat(yearWheelService.calculateOccurrenceMonths(2026, deadline, TaskRepetition.NONE, deadline))
				.isEmpty();
		assertThat(yearWheelService.calculateOccurrenceMonths(2027, deadline, TaskRepetition.NONE, deadline))
				.containsExactly(8);
	}

	@Test
	public void deadlineLateInAMonthKeepsItsFirstOccurrence() {
		// Completing a check due 31/01 steps the deadline to 28/02 (the day is clamped), so a bound that
		// compared exact dates would put the first occurrence one interval too late and lose January
		LocalDate firstDeadline = LocalDate.of(2026, 1, 31);

		assertThat(yearWheelService.calculateOccurrenceMonths(2026, LocalDate.of(2026, 2, 28), TaskRepetition.MONTHLY, firstDeadline))
				.containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
	}

	@Test
	public void leapDayDeadlineKeepsItsFirstOccurrence() {
		// 29/02-2024 steps to 28/02-2025, which is a day before the first deadline's day of month
		assertThat(yearWheelService.calculateOccurrenceMonths(2024, LocalDate.of(2025, 2, 28), TaskRepetition.YEARLY, LocalDate.of(2024, 2, 29)))
				.containsExactly(2);
	}

	@Test
	public void wheelLeavesOutTheYearsBeforeANewTasksFirstDeadline() {
		// The reported case: a yearly task first due 12/08-2027 must not surface in the 2026 wheel
		TaskGrid task = reportableTask(7L, LocalDate.of(2027, 8, 12), TaskRepetition.YEARLY);
		stubTasks(task);
		doReturn(Map.of()).when(taskService).getFirstDeadlines(List.of(7L));

		assertThat(occurrences(yearWheelService.buildYearWheel(2026, Map.of(), null, false))).isEmpty();
		assertThat(occurrences(yearWheelService.buildYearWheel(2027, Map.of(), null, false)))
				.extracting(YearWheelTaskDTO::getDeadline)
				.containsExactly("12/08-2027");
	}

	@Test
	public void wheelProjectsBackToTheLoggedFirstDeadline() {
		TaskGrid task = reportableTask(7L, LocalDate.of(2026, 8, 12), TaskRepetition.YEARLY);
		stubTasks(task);
		doReturn(Map.of(7L, LocalDate.of(2024, 8, 12))).when(taskService).getFirstDeadlines(List.of(7L));

		assertThat(occurrences(yearWheelService.buildYearWheel(2024, Map.of(), null, false)))
				.extracting(YearWheelTaskDTO::getDeadline)
				.containsExactly("12/08-2024");
		assertThat(occurrences(yearWheelService.buildYearWheel(2023, Map.of(), null, false))).isEmpty();
	}

	@Test
	public void wheelFallsBackToNextDeadlineWhenTheDeadlineWasMovedBackwards() {
		// An inspection date moved backwards leaves a logged deadline that is later than the next one
		TaskGrid task = reportableTask(7L, LocalDate.of(2026, 3, 1), TaskRepetition.YEARLY);
		stubTasks(task);
		doReturn(Map.of(7L, LocalDate.of(2027, 3, 1))).when(taskService).getFirstDeadlines(List.of(7L));

		assertThat(occurrences(yearWheelService.buildYearWheel(2026, Map.of(), null, false)))
				.extracting(YearWheelTaskDTO::getDeadline)
				.containsExactly("01/03-2026");
		assertThat(occurrences(yearWheelService.buildYearWheel(2025, Map.of(), null, false))).isEmpty();
	}

	@Test
	public void wheelSkipsTasksWithoutADeadline() {
		TaskGrid task = reportableTask(7L, null, TaskRepetition.YEARLY);
		stubTasks(task);

		assertThat(occurrences(yearWheelService.buildYearWheel(2026, Map.of(), null, false))).isEmpty();
	}

	private TaskGrid reportableTask(final Long id, final LocalDate nextDeadline, final TaskRepetition repetition) {
		TaskGrid task = new TaskGrid();
		task.setId(id);
		task.setName("Årlig opdatering behandlingsaktivitet nr. 50A");
		task.setTaskType(TaskType.CHECK);
		task.setTaskRepetition(repetition);
		task.setNextDeadline(nextDeadline != null ? nextDeadline.atStartOfDay() : null);
		task.setIncludeInReport(true);
		return task;
	}

	private void stubTasks(final TaskGrid... tasks) {
		Page<TaskGrid> page = new PageImpl<>(List.of(tasks));
		doReturn(page).when(taskService).getTasks(any(), any(), any(), anyInt(), anyInt(), any(), anyBoolean());
		doReturn(List.of()).when(tagService).findAll();
	}

	private List<YearWheelTaskDTO> occurrences(final YearWheelDTO wheel) {
		return wheel.getMonths().values().stream().flatMap(List::stream).toList();
	}
}

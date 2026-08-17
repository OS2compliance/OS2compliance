package dk.digitalidentity.service;

import dk.digitalidentity.model.entity.enums.TaskRepetition;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the occurrence projection behind the year wheel. The interesting cases are all about the
 * lower bound: the wheel projects a repeating task backwards from its next deadline, and must not project
 * past the first deadline the task ever had.
 */
public class YearWheelServiceTest {
	private final YearWheelService yearWheelService = new YearWheelService(null, null);

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
}

package dk.digitalidentity.statistic.model.ChartConfiguration;

import dk.digitalidentity.service.IncidentService;
import dk.digitalidentity.statistic.StatisticService;
import dk.digitalidentity.statistic.enumerable.DateTimePreset;
import dk.digitalidentity.statistic.enumerable.Period;
import dk.digitalidentity.statistic.enumerable.SelectablePeriod;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * A chart whose period/grouping isn't user-selectable must resolve those values from its own
 * configuration, never from client-supplied request parameters
 */
public class ChartConfigurationServiceTest {
	private final ChartConfigurationDao chartConfigurationDao = mock(ChartConfigurationDao.class);
	private final StatisticService statisticService = mock(StatisticService.class);
	private final IncidentService incidentService = mock(IncidentService.class);
	private final ChartConfigurationService chartConfigurationService = new ChartConfigurationService(chartConfigurationDao, statisticService, incidentService);

	@Test
	public void groupTimeByIsAlwaysTakenFromConfigWhenPeriodIsNotSelectable() {
		ChartConfiguration chartConfig = ChartConfiguration.builder()
				.selectablePeriod(SelectablePeriod.NONE)
				.groupTimeByField(null)
				.build();

		assertThat(chartConfigurationService.resolveGroupTimeBy(chartConfig, Period.MONTH)).isNull();
	}

	@Test
	public void groupTimeByIsIgnoredFromRequestEvenWhenItMatchesConfigButPeriodIsNotSelectable() {
		ChartConfiguration chartConfig = ChartConfiguration.builder()
				.selectablePeriod(SelectablePeriod.NONE)
				.groupTimeByField(Period.MONTH)
				.build();

		// A tampered request could send anything; the config's own value must win regardless of what matches
		assertThat(chartConfigurationService.resolveGroupTimeBy(chartConfig, Period.YEAR)).isEqualTo(Period.MONTH);
		assertThat(chartConfigurationService.resolveGroupTimeBy(chartConfig, null)).isEqualTo(Period.MONTH);
	}

	@Test
	public void groupTimeByRequiresRequestToMatchConfigWhenPeriodIsSelectable() {
		ChartConfiguration chartConfig = ChartConfiguration.builder()
				.selectablePeriod(SelectablePeriod.BOTH)
				.groupTimeByField(Period.MONTH)
				.build();

		assertThat(chartConfigurationService.resolveGroupTimeBy(chartConfig, Period.MONTH)).isEqualTo(Period.MONTH);
		assertThat(chartConfigurationService.resolveGroupTimeBy(chartConfig, Period.YEAR)).isNull();
		assertThat(chartConfigurationService.resolveGroupTimeBy(chartConfig, null)).isNull();
	}

	@Test
	public void startAndEndDateFallBackToConfigDefaultsWhenPeriodIsNotSelectable() {
		ChartConfiguration chartConfig = ChartConfiguration.builder()
				.selectablePeriod(SelectablePeriod.NONE)
				.defaultStartTime(DateTimePreset.YEAR_START)
				.defaultEndTime(DateTimePreset.YEAR_END)
				.build();

		LocalDate now = LocalDate.now();
		LocalDate tamperedStart = now.minusYears(5);
		LocalDate tamperedEnd = now.plusYears(5);

		assertThat(chartConfigurationService.resolveStartDate(chartConfig, tamperedStart))
				.isEqualTo(now.with(TemporalAdjusters.firstDayOfYear()));
		assertThat(chartConfigurationService.resolveEndDate(chartConfig, tamperedEnd))
				.isEqualTo(now.with(TemporalAdjusters.lastDayOfYear()));
	}

	@Test
	public void startAndEndDateUseRequestValueWhenPeriodIsSelectable() {
		ChartConfiguration chartConfig = ChartConfiguration.builder()
				.selectablePeriod(SelectablePeriod.BOTH)
				.defaultStartTime(DateTimePreset.YEAR_START)
				.defaultEndTime(DateTimePreset.YEAR_END)
				.build();

		LocalDate requestedStart = LocalDate.of(2020, 3, 1);
		LocalDate requestedEnd = LocalDate.of(2020, 9, 1);

		assertThat(chartConfigurationService.resolveStartDate(chartConfig, requestedStart)).isEqualTo(requestedStart);
		assertThat(chartConfigurationService.resolveEndDate(chartConfig, requestedEnd)).isEqualTo(requestedEnd);
	}

	@Test
	public void startDateRespectsStartOnlyAndEndDateRespectsEndOnly() {
		ChartConfiguration startOnly = ChartConfiguration.builder()
				.selectablePeriod(SelectablePeriod.START_ONLY)
				.defaultStartTime(DateTimePreset.YEAR_START)
				.defaultEndTime(DateTimePreset.YEAR_END)
				.build();
		ChartConfiguration endOnly = ChartConfiguration.builder()
				.selectablePeriod(SelectablePeriod.END_ONLY)
				.defaultStartTime(DateTimePreset.YEAR_START)
				.defaultEndTime(DateTimePreset.YEAR_END)
				.build();

		LocalDate requestedStart = LocalDate.of(2020, 3, 1);
		LocalDate requestedEnd = LocalDate.of(2020, 9, 1);
		LocalDate now = LocalDate.now();

		assertThat(chartConfigurationService.resolveStartDate(startOnly, requestedStart)).isEqualTo(requestedStart);
		assertThat(chartConfigurationService.resolveEndDate(startOnly, requestedEnd))
				.isEqualTo(now.with(TemporalAdjusters.lastDayOfYear()));

		assertThat(chartConfigurationService.resolveStartDate(endOnly, requestedStart))
				.isEqualTo(now.with(TemporalAdjusters.firstDayOfYear()));
		assertThat(chartConfigurationService.resolveEndDate(endOnly, requestedEnd)).isEqualTo(requestedEnd);
	}
}

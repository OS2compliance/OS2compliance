package dk.digitalidentity.statistic.model.ChartConfiguration;

import dk.digitalidentity.model.entity.IncidentField;
import dk.digitalidentity.service.IncidentService;
import dk.digitalidentity.statistic.StatisticService;
import dk.digitalidentity.statistic.dto.ChartConfigurationDTO;
import dk.digitalidentity.statistic.dto.EntityFieldChoiceDTO;
import dk.digitalidentity.statistic.dto.ErrorDTO;
import dk.digitalidentity.statistic.enumerable.DateTimePreset;
import dk.digitalidentity.statistic.enumerable.Period;
import dk.digitalidentity.statistic.enumerable.SelectablePeriod;
import dk.digitalidentity.statistic.interfaces.StatisticEnabled;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class ChartConfigurationService {
	private final ChartConfigurationDao chartConfigurationDao;
	private final StatisticService statisticService;
	private final IncidentService incidentService;

	public Optional<ChartConfiguration> findById(Long id) {
		return chartConfigurationDao.findById(id);
	}

	public Optional<ChartConfiguration> findByName(String name) {
		return chartConfigurationDao.findByName(name);
	}

	public List<ChartConfiguration> saveAll(List<ChartConfiguration> chartConfigurations) {
		return chartConfigurationDao.saveAll(chartConfigurations);
	}

	public List<ChartConfiguration> getChartConfigurationsForSection(String section) {
		return chartConfigurationDao.findAllBySection(section);
	}

	public ChartConfigurationDTO toDTO(ChartConfiguration chartConfig, Class<? extends StatisticEnabled> entityClass) {
		Set<ErrorDTO> errors = new HashSet<>();
		List<EntityFieldChoiceDTO> allowedXFieldChoices;
		List<EntityFieldChoiceDTO> allowedYFieldChoices;

		boolean showYOverride = false;
		if (chartConfig.getEntityName().equalsIgnoreCase("Incident")) {
			// special case for incidents, where allowed fields are generated from obligatory incident fields
			List<IncidentField> incidentFields = incidentService.getAllObligatoryFields();
			if (incidentFields.isEmpty()) {
				allowedYFieldChoices = List.of();
				showYOverride = true;
				errors.add(new ErrorDTO(
						ErrorDTO.Regarding.YAXIS,
						"Ingen obligatoriske felter fundet. Statistik kan kun laves på obligatoriske felter."));
			} else {
				allowedYFieldChoices = incidentFields.stream()
						.map(i -> new EntityFieldChoiceDTO(
								i.getIndexColumnName(),
								i.getIndexColumnName(),
								i.getId().toString())).toList();
			}

			allowedXFieldChoices = chartConfig.getAllowedXFieldChoices().stream()
					.map(s -> new EntityFieldChoiceDTO(
							s,
							statisticService.getStatisticLabelForField(entityClass, s).orElse(s),
							null))
					.toList();

		}
		else {
			allowedYFieldChoices = chartConfig.getAllowedYFieldChoices().stream()
					.map(s -> new EntityFieldChoiceDTO(s, statisticService.getStatisticLabelForField(entityClass, s).orElse(s), null)).toList();
			allowedXFieldChoices = chartConfig.getAllowedXFieldChoices().stream()
					.map(s -> new EntityFieldChoiceDTO(s, statisticService.getStatisticLabelForField(entityClass, s).orElse(s), null)).toList();
		}

		// Calculate showing of Axis fields
		boolean showX = false;
		boolean showY = false;
		switch (chartConfig.getSelectableAxis()) {
			case BOTH -> {
				showX = !allowedXFieldChoices.isEmpty();
				showY = !allowedYFieldChoices.isEmpty();
			}
			case X_ONLY -> showX = !allowedXFieldChoices.isEmpty();
			case Y_ONLY -> showY = showYOverride || !allowedYFieldChoices.isEmpty();
			default -> {
				// None of the axes should show
			}
		}

		// Calculate time fields
		LocalDate defaultStartTime = toLocalDate(chartConfig.getDefaultStartTime());
		LocalDate defaultEndTime = toLocalDate(chartConfig.getDefaultEndTime());
		boolean showStartTime = false;
		boolean showEndTime = false;
		switch (chartConfig.getSelectablePeriod()) {
			case BOTH -> {
				showStartTime = true;
				showEndTime = true;
			}
			case START_ONLY -> showStartTime = true;
			case END_ONLY -> showEndTime = true;
			default -> {
				// Both should be false
			}
		}

		boolean yFieldFromXField = allowedYFieldChoices.isEmpty();
		boolean xFieldFromYField = allowedXFieldChoices.isEmpty();

		boolean showGroupTime = chartConfig.getGroupTimeByField() != null && (showStartTime || showEndTime);

		return ChartConfigurationDTO.builder()
				.id(chartConfig.getId())
				.name(chartConfig.getName())
				.entityName(chartConfig.getEntityName())
				.ownerOnly(chartConfig.getOwnerOnly())
				.aggregation(chartConfig.getAggregation())
				.type(chartConfig.getType())
				.showXField(showX)
				.allowedXFieldChoices(allowedXFieldChoices)
				.showYField(showY)
				.allowedYFieldChoices(allowedYFieldChoices)
				.showStartTime(showStartTime)
				.defaultStartTime(defaultStartTime)
				.showEndTime(showEndTime)
				.defaultEndTime(defaultEndTime)
				.showGroupTime(showGroupTime)
				.groupTimeByField(chartConfig.getGroupTimeByField())
				.yFieldFromXField(yFieldFromXField)
				.xFieldFromYField(xFieldFromYField)
				.errors(errors)
				.build();
	}

	/**
	 * Resolves the effective time-grouping for a chart. When the config doesn't let the user select
	 * a period, {@code groupTimeByField} is the only valid source
	 * @param chartConfig       the chart's configuration
	 * @param requestedGroupTimeBy grouping requested by the client, if any
	 * @return the grouping to actually use, or null for none
	 */
	public Period resolveGroupTimeBy(ChartConfiguration chartConfig, Period requestedGroupTimeBy) {
		if (chartConfig.getSelectablePeriod() == SelectablePeriod.NONE) {
			return chartConfig.getGroupTimeByField();
		}
		if (requestedGroupTimeBy == null || chartConfig.getGroupTimeByField() == null || chartConfig.getGroupTimeByField() != requestedGroupTimeBy) {
			return null;
		}
		return requestedGroupTimeBy;
	}

	/**
	 * Resolves the effective start date for a chart. When the start date isn't user-selectable per
	 * {@code selectablePeriod}, the config's own default is used instead of a client-sent value.
	 */
	public LocalDate resolveStartDate(ChartConfiguration chartConfig, LocalDate requestedStartDate) {
		boolean showStartTime = chartConfig.getSelectablePeriod() == SelectablePeriod.BOTH || chartConfig.getSelectablePeriod() == SelectablePeriod.START_ONLY;
		return showStartTime ? requestedStartDate : toLocalDate(chartConfig.getDefaultStartTime());
	}

	/**
	 * Resolves the effective end date for a chart. When the end date isn't user-selectable per
	 * {@code selectablePeriod}, the config's own default is used instead of a client-sent value.
	 */
	public LocalDate resolveEndDate(ChartConfiguration chartConfig, LocalDate requestedEndDate) {
		boolean showEndTime = chartConfig.getSelectablePeriod() == SelectablePeriod.BOTH || chartConfig.getSelectablePeriod() == SelectablePeriod.END_ONLY;
		return showEndTime ? requestedEndDate : toLocalDate(chartConfig.getDefaultEndTime());
	}

	/**
	 * Maps a preset to a local date relative to right now
	 * @param preset DateTimePreset
	 * @return LocalDate relative to time right now
	 */
	private LocalDate toLocalDate(DateTimePreset preset) {
		LocalDate now = LocalDate.now();
		int quarterStartMonth = ((now.getMonthValue() - 1) / 3) * 3 + 1;
		return switch (preset) {
			case YEAR_START -> now.with(TemporalAdjusters.firstDayOfYear());
			case YEAR_END -> now.with(TemporalAdjusters.lastDayOfYear());
			case QUARTER_START -> now.withMonth(quarterStartMonth)
					.with(TemporalAdjusters.firstDayOfMonth());
			case QUARTER_END -> now.withMonth(quarterStartMonth)
					.plusMonths(2)
					.with(TemporalAdjusters.lastDayOfMonth());
			case MONTH_START -> now.with(TemporalAdjusters.firstDayOfMonth());
			case MONTH_END -> now.with(TemporalAdjusters.lastDayOfMonth());
			case CURRENT_TIME -> now;
			default -> null;
		};
	}
}

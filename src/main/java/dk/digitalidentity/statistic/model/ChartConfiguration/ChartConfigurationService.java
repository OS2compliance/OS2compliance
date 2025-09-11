package dk.digitalidentity.statistic.model.ChartConfiguration;

import dk.digitalidentity.model.entity.IncidentField;
import dk.digitalidentity.service.IncidentService;
import dk.digitalidentity.statistic.StatisticService;
import dk.digitalidentity.statistic.dto.ChartConfigurationDTO;
import dk.digitalidentity.statistic.dto.EntityFieldChoiceDTO;
import dk.digitalidentity.statistic.enumerable.DateTimePreset;
import dk.digitalidentity.statistic.interfaces.StatisticEnabled;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ChartConfigurationService {
	private final ChartConfigurationDao chartConfigurationDao;
	private final StatisticService statisticService;
	private final IncidentService incidentService;

	public Optional<ChartConfiguration> findById(Long id) {
		return chartConfigurationDao.findById(id);
	}

	public List<ChartConfiguration> saveAll(List<ChartConfiguration> chartConfigurations) {
		return chartConfigurationDao.saveAll(chartConfigurations);
	}

	public List<ChartConfiguration> getChartConfigurationsForSection(String section) {
		return chartConfigurationDao.findAllBySection(section);
	}

	public ChartConfigurationDTO toDTO(ChartConfiguration chartConfig, Class<? extends StatisticEnabled> entityClass) {
		List<EntityFieldChoiceDTO> allowedXFieldChoices;
		List<EntityFieldChoiceDTO> allowedYFieldChoices;

		if (chartConfig.getEntityName().equalsIgnoreCase("Incident")) {
			// special case for incidents, where allowed fields are generated from obligatory incident fields
			List<IncidentField> incidentFields = incidentService.getAllObligatoryFields();
			allowedYFieldChoices = incidentFields.stream()
					.map(i -> new EntityFieldChoiceDTO(
							i.getIndexColumnName(),
							i.getIndexColumnName(),
							i.getId().toString())).toList();

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
			case Y_ONLY -> showY = !allowedYFieldChoices.isEmpty();
			default -> {
				// None of the axes should show
			}
		}

		// Calculate time fields
		LocalDateTime defaultStartTime = toLocalDateTime(chartConfig.getDefaultStartTime());
		LocalDateTime defaultEndTime = toLocalDateTime(chartConfig.getDefaultEndTime());
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
				.build();
	}

	private LocalDateTime toLocalDateTime(DateTimePreset preset) {
		LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
		LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
		LocalDateTime endOfDay = now.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
		int quarterStartMonth = ((now.getMonthValue() - 1) / 3) * 3 + 1;
		return switch (preset) {
			case YEAR_START -> startOfDay.with(TemporalAdjusters.firstDayOfYear());
			case YEAR_END -> endOfDay.with(TemporalAdjusters.lastDayOfYear());
			case QUARTER_START -> startOfDay.withMonth(quarterStartMonth)
					.with(TemporalAdjusters.firstDayOfMonth());
			case QUARTER_END -> endOfDay.withMonth(quarterStartMonth)
					.plusMonths(2)
					.with(TemporalAdjusters.lastDayOfMonth());
			case MONTH_START -> startOfDay.with(TemporalAdjusters.firstDayOfMonth());
			case MONTH_END -> endOfDay.with(TemporalAdjusters.lastDayOfMonth());
			case DAY_START -> startOfDay;
			case DAY_END -> endOfDay;
			case CURRENT_TIME -> now;
			default -> null;
		};
	}
}

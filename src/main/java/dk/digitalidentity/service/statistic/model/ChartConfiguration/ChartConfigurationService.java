package dk.digitalidentity.service.statistic.model.ChartConfiguration;

import dk.digitalidentity.service.statistic.StatisticService;
import dk.digitalidentity.service.statistic.dto.ChartConfigurationDTO;
import dk.digitalidentity.service.statistic.dto.EntityFieldChoiceDTO;
import dk.digitalidentity.service.statistic.interfaces.StatisticEnabled;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ChartConfigurationService {
	private final ChartConfigurationDao chartConfigurationDao;
	private final StatisticService statisticService;

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
		return ChartConfigurationDTO.builder()
				.id(chartConfig.getId())
				.name(chartConfig.getName())
				.entityName(chartConfig.getEntityName())
				.ownerOnly(chartConfig.getOwnerOnly())
				.aggregation(chartConfig.getAggregation())
				.type(chartConfig.getType())
				.allowedXFieldChoices(chartConfig.getAllowedXFieldChoices().stream().map(s -> new EntityFieldChoiceDTO(s, statisticService.getStatisticLabelForField(entityClass, s).orElse(s))).toList())
				.allowedYFieldChoices(chartConfig.getAllowedYFieldChoices().stream().map(s -> new EntityFieldChoiceDTO(s, statisticService.getStatisticLabelForField(entityClass, s).orElse(s))).toList())
				.defaultStartTime(chartConfig.getDefaultStartTime())
				.defaultEndTime(chartConfig.getDefaultEndTime())
				.groupTimeByField(chartConfig.getGroupTimeByField())
				.build();
	}
}

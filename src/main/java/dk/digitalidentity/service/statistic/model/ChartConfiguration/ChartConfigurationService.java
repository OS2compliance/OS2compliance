package dk.digitalidentity.service.statistic.model.ChartConfiguration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ChartConfigurationService {
	private final ChartConfigurationDao chartConfigurationDao;

	public List<ChartConfiguration> saveAll(List<ChartConfiguration> chartConfigurations) {
		return chartConfigurationDao.saveAll(chartConfigurations);
	}


	public List<ChartConfiguration> getChartConfigurationsForSection(String entityName) {
		return chartConfigurationDao.findAllByEntityName(entityName);
	}
}

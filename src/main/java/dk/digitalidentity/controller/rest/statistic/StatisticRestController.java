package dk.digitalidentity.controller.rest.statistic;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.service.statistic.dto.chartJS.ChartJsConfigDTO;
import dk.digitalidentity.service.statistic.dto.chartJS.ChartJsDataDTO;
import dk.digitalidentity.service.statistic.enumerable.Period;
import dk.digitalidentity.service.statistic.interfaces.StatisticEnabled;
import dk.digitalidentity.service.statistic.StatisticService;
import dk.digitalidentity.service.statistic.model.ChartConfiguration.ChartConfiguration;
import dk.digitalidentity.service.statistic.model.ChartConfiguration.ChartConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("rest/statistic")
@RequiredArgsConstructor
public class StatisticRestController {
	private final StatisticService statisticService;
	private final ChartConfigurationService chartConfigurationService;

	private final Map<String, Class<? extends StatisticEnabled>> entityMap = Map.of
			(
			"asset", Asset.class,
			"incident", Incident.class,
			"task", Task.class,
			"dpia", DPIA.class,
			"threatassessment", ThreatAssessment.class
	);

	@GetMapping("{chartId}")
	public ResponseEntity<ChartJsConfigDTO> getChart(
			@PathVariable Long chartId,
			@RequestParam(required = false) final String x, // X-axis field, usually for label
			@RequestParam(required = false) final String y, // Y-axis field, usually for values
			@RequestParam(required = false) final Period groupTimeBy,
			@RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM-yyyy HH:mm:ss") final LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM-yyyy HH:mm:ss") final LocalDate endDate
	) {

		ChartConfiguration chartConfig = chartConfigurationService.findById(chartId)
				.orElseThrow();

		Class<? extends StatisticEnabled> entityClass = entityMap.get(chartConfig.getEntityName().toLowerCase());

		String xField = x;
		String yField = y;
		if (chartConfig.getAllowedYFieldChoices().isEmpty()) {
			// if no value-fields are allowed, it should be the same as the label field
			yField = xField;
		}

		if (entityClass == null
				|| xField == null
				|| yField == null
		) {
			return ResponseEntity.badRequest().build();
		}

		ChartJsDataDTO chartData = statisticService.generateChart(entityClass, chartConfig.getType(), x, y, chartConfig.getAggregation(), chartConfig.getOwnerOnly(), groupTimeBy, chartConfig.getAllowedDateFieldChoices().stream().findFirst().orElse(null), startDate, endDate);
		return ResponseEntity.ok(ChartJsConfigDTO.builder()
						.title(chartConfig.getName())
						.type(chartConfig.getType())
						.data(chartData)
				.build());

	}



}

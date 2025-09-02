package dk.digitalidentity.controller.rest.statistic;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.service.statistic.ChartJsDataDTO;
import dk.digitalidentity.service.statistic.ChartType;
import dk.digitalidentity.service.statistic.Period;
import dk.digitalidentity.service.statistic.StatisticEnabled;
import dk.digitalidentity.service.statistic.StatisticService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("rest/statistic")
@RequiredArgsConstructor
public class StatisticRestController {
	private final StatisticService statisticService;

	private final Map<String, Class<? extends StatisticEnabled>> entityMap = Map.of(
			"asset", Asset.class,
			"incident", Incident.class,
			"task", Task.class,
			"dpia", DPIA.class,
			"threatassessment", ThreatAssessment.class
	);

	@GetMapping("{entityName}")
	public ResponseEntity<ChartJsDataDTO> getChart(
			@PathVariable String entityName,
			@RequestParam ChartType type,
			@RequestParam String x, // X-axis field, usually for label
			@RequestParam String y, // Y-axis field, usually for values
			@RequestParam(required = false) String stack, // Stack field for stacked charts
			@RequestParam(defaultValue = "count") String aggregation,
			@RequestParam(required = false) Period groupTimeBy,
			@RequestParam(required = false) Boolean ownerOnly,
			@RequestParam(required = false) String dateField, // Date field for filtering
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
	) {

		Class<? extends StatisticEnabled> entityClass = entityMap.get(entityName);

		if (entityClass == null
				|| type == null
				|| x == null
				|| y == null
		) {
			return ResponseEntity.badRequest().build();
		}

		ChartJsDataDTO chartData = statisticService.generateChart(entityClass, type, x, y, stack, aggregation, ownerOnly, groupTimeBy, dateField, startDate, endDate);
		return ResponseEntity.ok(chartData);

	}



}

package dk.digitalidentity.controller.mvc.statistic;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.security.annotations.crud.RequireReadAll;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.DPIAService;
import dk.digitalidentity.service.IncidentService;
import dk.digitalidentity.service.TaskService;
import dk.digitalidentity.service.ThreatAssessmentService;
import dk.digitalidentity.statistic.interfaces.StatisticEnabled;
import dk.digitalidentity.statistic.model.ChartConfiguration.ChartConfiguration;
import dk.digitalidentity.statistic.model.ChartConfiguration.ChartConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

// No section security annotation as this is accessible for many different sections
@Slf4j
@Controller
@RequestMapping("statistic")
@RequiredArgsConstructor
public class StatisticController {
	private final ChartConfigurationService chartConfigurationService;
	private final AssetService assetService;
	private final TaskService taskService;
	private final DPIAService dPIAService;
	private final ThreatAssessmentService threatAssessmentService;
	private final IncidentService incidentService;

	public enum StatisticSupportedPages {
		ASSET, INCIDENT, TASK, DPIA, THREAT_ASSESSMENT
	}

	private final Map<String, Class<? extends StatisticEnabled>> entityMap = Map.of
			(
					"asset".toLowerCase(), Asset.class,
					"incident".toLowerCase(), Incident.class,
					"task".toLowerCase(), Task.class,
					"dpia".toLowerCase(), DPIA.class,
					"threatAssessment".toLowerCase(), ThreatAssessment.class
			);

	private final Set<String> statisticSupportedSections = Set.of(
			"dashboard",
			"asset",
			"incident",
			"task",
			"dpia",
			"threatassessment"
	);

	public record DiagramConfigDTO(String name, String entityName, Long id) {
	}

	@RequireReadAll
	@GetMapping("{section}")
	public String getModal(final Model model, @PathVariable("section") String section) {

		String lowercaseSection = section.toLowerCase();
		if (!statisticSupportedSections.contains(lowercaseSection)) {
			throw new IllegalArgumentException();
		}

		List<ChartConfiguration> chartConfigs = chartConfigurationService.getChartConfigurationsForSection(lowercaseSection);

		model.addAttribute("availableDiagrams", chartConfigs.stream()
				.map(chartConfig -> new DiagramConfigDTO(
						chartConfig.getName(),
						chartConfig.getEntityName(),
						chartConfig.getId()))
				.toList()
		);

		return "statistic/pageView/" + lowercaseSection;
	}

	@GetMapping("chart/{entityName}/{id}")
	public String getDiagramConfig(final Model model, @PathVariable String entityName, @PathVariable Long id) {

		ChartConfiguration chartConfig = chartConfigurationService.findById(id)
				.orElseThrow();

		model.addAttribute("config", chartConfigurationService.toDTO(chartConfig, entityMap.get(entityName.toLowerCase())));
		return "statistic/fragment/chartConfig";
	}

	record EntityListDTO(String name, String url) {
	}

	@GetMapping("chart/{entityName}/entityList")
	public String getRelevantEntitiesForChart(
			final Model model,
			@PathVariable String entityName,
			@RequestParam List<Long> entityIds
	) {
		String lowerCaseEntityName = entityName.toLowerCase();

		if (!entityMap.containsKey(lowerCaseEntityName)) {
			throw new NoSuchElementException("Entity name not found: " + lowerCaseEntityName);
		}
		List<EntityListDTO> entities = switch (lowerCaseEntityName) {
			case "asset" -> assetService.getByIds(entityIds).stream().map(e -> new EntityListDTO(e.getName(), "/assets/" + e.getId())).toList();
			case "incident" -> incidentService.getByIds(entityIds).stream().map(e -> new EntityListDTO(e.getName(), "/incidents/logs/" + e.getId())).toList();
			case "task" -> taskService.getByIds(entityIds).stream().map(e -> new EntityListDTO(e.getName(), "/tasks/" + e.getId())).toList();
			case "dpia" -> dPIAService.getByIds(entityIds).stream().map(e -> new EntityListDTO(e.getName(), "/dpia/" + e.getId())).toList();
			case "threatassessment" -> threatAssessmentService.getByIds(entityIds).stream().map(e -> new EntityListDTO(e.getName(), "/risks/" + e.getId())).toList();
			default -> throw new IllegalArgumentException();
		};

		model.addAttribute("entities", entities);
		return "statistic/fragment/entityListView :: entityListView";
	}
}

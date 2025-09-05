package dk.digitalidentity.controller.mvc.statistic;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.security.annotations.crud.RequireReadAll;
import dk.digitalidentity.service.statistic.interfaces.StatisticEnabled;
import dk.digitalidentity.service.statistic.model.ChartConfiguration.ChartConfiguration;
import dk.digitalidentity.service.statistic.model.ChartConfiguration.ChartConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.Set;

// No section security annotation as this is accessible for many different sections
@Slf4j
@Controller
@RequestMapping("statistic")
@RequiredArgsConstructor
public class StatisticController {
	private final ChartConfigurationService chartConfigurationService;

	public enum StatisticSupportedPages {
		ASSET, INCIDENT, TASK, DPIA, THREAT_ASSESSMENT
	}

	private final Set<String> statisticSupportedSections = Set.of(
			"asset",
			"incident",
			"task",
			"dpia",
			"threatassessment"
	);

	private final Map<String, Class<? extends StatisticEnabled>> entityMap = Map.of(
			"asset", Asset.class,
			"incident", Incident.class,
			"task", Task.class,
			"dpia", DPIA.class,
			"threatassessment", ThreatAssessment.class
	);

	@RequireReadAll
	@GetMapping
	public String getBaseView(final Model model) {

		return "statistic/base";
	}

	public record DiagramConfigDTO(String name, String entityName, Long id) {
	}

	@RequireReadAll
	@GetMapping("{section}")
	public String getModal(final Model model, @PathVariable("section") String section) {

		if (section == null || !statisticSupportedSections.contains(section)) {
			throw new IllegalArgumentException();
		}

		List<ChartConfiguration> chartConfigs = chartConfigurationService.getChartConfigurationsForSection(section);

		model.addAttribute("availableDiagrams", chartConfigs.stream()
				.map(chartConfig -> new DiagramConfigDTO(
						chartConfig.getName(),
						chartConfig.getEntityName(),
						chartConfig.getId()))
		);

		return "statistic/pageView/" + section;
	}

	@GetMapping("chart/{entityName}/{id}")
	public String getDiagramConfig(final Model model, @PathVariable String entityName, @PathVariable Long id) {

		ChartConfiguration chartConfig = chartConfigurationService.findById(id)
				.orElseThrow();


		model.addAttribute("config", chartConfigurationService.toDTO(chartConfig, entityMap.get(entityName.toLowerCase())));
		return "statistic/fragment/chartConfig";
	}
}

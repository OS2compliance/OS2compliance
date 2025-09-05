package dk.digitalidentity.controller.mvc.statistic;

import dk.digitalidentity.security.annotations.crud.RequireReadAll;
import dk.digitalidentity.service.statistic.model.enumerable.ChartType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Set;

// No section security annotation as this is accessible for many different sections
@Slf4j
@Controller
@RequestMapping("statistic")
@RequiredArgsConstructor
public class StatisticController {

	public enum StatisticSupportedPages {
		ASSET,	INCIDENT, TASK, DPIA, THREAT_ASSESSMENT
	}

	private final Set<String> statisticSupportedSections = Set.of(
			"asset",
			"incident",
			"task",
			"dpia",
			"threatassessment"
	);

	public record DiagramConfigDTO(String name, ChartType type, Long id) {}

	@RequireReadAll
	@GetMapping
	public String getBaseView(final Model model) {

		return "statistic/base";
	}

	@RequireReadAll
	@GetMapping("{section}")
	public String getModal(final Model model, @PathVariable("section") String section) {

		if (section == null || !statisticSupportedSections.contains(section)) {
			throw new IllegalArgumentException();
		}

		model.addAttribute("availableDiagrams", List.of(new DiagramConfigDTO("test1", ChartType.BAR, 1L), new DiagramConfigDTO("test2", ChartType.PIE, 2L)));

		return "statistic/pageView/" + section;
	}
}

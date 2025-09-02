package dk.digitalidentity.controller.mvc.statistic;

import dk.digitalidentity.security.annotations.crud.RequireReadAll;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// No section security annotation as this is accessible for many different sections
@Slf4j
@Controller
@RequestMapping("statistic")
@RequiredArgsConstructor
public class StatisticController {



	@RequireReadAll
	@GetMapping
	public String getBaseView(final Model model) {

		return "statistic/base";
	}
}

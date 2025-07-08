package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.dto.SelectionDTO;
import dk.digitalidentity.service.kle.KLEGroupService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.Set;

@AllArgsConstructor
@Controller
@RequestMapping("kle")
public class KLEController {
	private final KLEGroupService kLEGroupService;

	@GetMapping("maingroup/groups")
	public String getGroupsForMainGroup(@RequestParam Set<String> mainGroupNumbers, @RequestParam(required = false) Set<String> selectedGroups, Model model) {

		model.addAttribute("kleGroups",
				kLEGroupService.findAllByMainGroupNumbers(mainGroupNumbers).stream()
						.map(g -> new SelectionDTO(
								g.getGroupNumber() + " " + g.getTitle(),
								g.getGroupNumber(),
								selectedGroups.stream().anyMatch(nr -> g.getGroupNumber().equals(nr))))
						.sorted(Comparator.comparing(SelectionDTO::value))
						.toList()
		);

		return "kle/fragment/kleGroupOptions";
	}
}

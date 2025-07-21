package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.dto.SelectionDTO;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.kle.KLEGroupService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

@RequireUser
@AllArgsConstructor
@Controller
@RequestMapping("kle")
public class KLEController {
	private final KLEGroupService kLEGroupService;

	/**
	 * Collected view strings, for maintainability
	 */
	private enum VIEW {
		KLEGROUPOPTIONS("kle/fragment/kleGroupOptions :: kleGroupOptions");

		private final String name;
		VIEW(String name) {
			this.name = name;
		}
	}

	@GetMapping("maingroup/groups")
	public String getGroupsForMainGroup(@RequestParam Set<String> mainGroupNumbers, @RequestParam(required = false) Set<String> selectedGroups, Model model) {

		if (mainGroupNumbers == null || mainGroupNumbers.isEmpty()) {
			model.addAttribute("mainGroupNumbers", new ArrayList<>());
			return VIEW.KLEGROUPOPTIONS.name;
		}

		Set<String> selectedGroupNumbers = selectedGroups == null ? new HashSet<>() : selectedGroups;
		model.addAttribute("kleGroups",
				kLEGroupService.findAllByMainGroupNumbers(mainGroupNumbers).stream()
						.map(g -> new SelectionDTO(
								g.getGroupNumber() + " " + g.getTitle(),
								g.getGroupNumber(),
								selectedGroupNumbers.stream().anyMatch(nr -> g.getGroupNumber().equals(nr))))
						.sorted(Comparator.comparing(SelectionDTO::value))
						.toList()
		);

		return VIEW.KLEGROUPOPTIONS.name;
	}
}

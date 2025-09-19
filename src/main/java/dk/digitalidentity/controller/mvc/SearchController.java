package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.service.GlobalSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;

@Controller
public class SearchController {

	@Autowired
	private GlobalSearchService searchService;

	@GetMapping("/search-results")
	public String searchResults(@RequestParam("q") String query, Model model) {
		model.addAttribute("searchQuery", query);

		if (query == null || query.trim().isEmpty()) {
			model.addAttribute("searchResults", Collections.emptyMap());
			return "search-results";
		}

		model.addAttribute("searchResults", searchService.search(query));
		return "search-results";
	}


}
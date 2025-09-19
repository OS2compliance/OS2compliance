package dk.digitalidentity.controller.rest;

import dk.digitalidentity.service.GlobalSearchService;
import dk.digitalidentity.service.GlobalSearchService.SearchResultSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("rest/search")
public class SearchRestController {

	@Autowired
	private GlobalSearchService searchService;

	@GetMapping("/more")
	public ResponseEntity<SearchResultSection> getMoreResults(
			@RequestParam("q") String query,
			@RequestParam("section") String sectionKey,
			@RequestParam(value = "page", defaultValue = "1") int page) {

		if (query == null || query.trim().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		Pageable pageable = PageRequest.of(page, 5);
		SearchResultSection section = searchService.searchInSection(query, sectionKey, pageable);

		if (section == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(section);
	}
}
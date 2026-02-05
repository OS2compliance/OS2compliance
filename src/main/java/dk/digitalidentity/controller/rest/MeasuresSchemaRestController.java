package dk.digitalidentity.controller.rest.Assets;

import dk.digitalidentity.model.entity.ChoiceMeasure;
import dk.digitalidentity.model.entity.ChoiceMeasureCategory;
import dk.digitalidentity.security.annotations.sections.RequireConfiguration;
import dk.digitalidentity.service.ChoiceMeasureCategoryService;
import dk.digitalidentity.service.ChoiceMeasuresService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequireConfiguration
@RequestMapping("rest/assets/measures/schema")
@RequiredArgsConstructor
public class MeasuresSchemaRestController {
	private final ChoiceMeasureCategoryService categoryService;
	private final ChoiceMeasuresService measuresService;

	// Category endpoints
	@PostMapping("category/{id}/up")
	@Transactional
	public void categorySortUp(@PathVariable final Long id) {
		ChoiceMeasureCategory category = categoryService.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		List<ChoiceMeasureCategory> all = categoryService.findAll().stream()
				.sorted(Comparator.comparing(ChoiceMeasureCategory::getSortOrder)
						.thenComparing(ChoiceMeasureCategory::getId))
				.collect(Collectors.toList());

		int index = findCategoryIndex(all, id);
		if (index > 0) {
			swapAndReassignCategories(all, index, index - 1);
		}
	}

	@PostMapping("category/{id}/down")
	@Transactional
	public void categorySortDown(@PathVariable final Long id) {
		ChoiceMeasureCategory category = categoryService.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		List<ChoiceMeasureCategory> all = categoryService.findAll().stream()
				.sorted(Comparator.comparing(ChoiceMeasureCategory::getSortOrder)
						.thenComparing(ChoiceMeasureCategory::getId))
				.collect(Collectors.toList());

		int index = findCategoryIndex(all, id);
		if (index >= 0 && index < all.size() - 1) {
			swapAndReassignCategories(all, index, index + 1);
		}
	}

	@DeleteMapping("category/{id}/delete")
	@Transactional
	public void categoryDelete(@PathVariable final Long id) {
		ChoiceMeasureCategory category = categoryService.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		categoryService.delete(category);
	}

	// Measure endpoints
	@PostMapping("measure/{id}/up")
	@Transactional
	public void measureSortUp(@PathVariable final Long id) {
		ChoiceMeasure measure = measuresService.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		List<ChoiceMeasure> allInCategory = measure.getCategory().getMeasures().stream()
				.filter(m -> !m.getDeleted())
				.sorted(Comparator.comparing(ChoiceMeasure::getSortOrder)
						.thenComparing(ChoiceMeasure::getId))
				.collect(Collectors.toList());

		int index = findMeasureIndex(allInCategory, id);
		if (index > 0) {
			swapAndReassignMeasures(allInCategory, index, index - 1);
		}
	}

	@PostMapping("measure/{id}/down")
	@Transactional
	public void measureSortDown(@PathVariable final Long id) {
		ChoiceMeasure measure = measuresService.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		List<ChoiceMeasure> allInCategory = measure.getCategory().getMeasures().stream()
				.filter(m -> !m.getDeleted())
				.sorted(Comparator.comparing(ChoiceMeasure::getSortOrder)
						.thenComparing(ChoiceMeasure::getId))
				.collect(Collectors.toList());

		int index = findMeasureIndex(allInCategory, id);
		if (index >= 0 && index < allInCategory.size() - 1) {
			swapAndReassignMeasures(allInCategory, index, index + 1);
		}
	}

	@DeleteMapping("measure/{id}/delete")
	@Transactional
	public void measureDelete(@PathVariable final Long id) {
		ChoiceMeasure measure = measuresService.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		measuresService.delete(measure);
	}

	// Helper methods for categories
	private int findCategoryIndex(List<ChoiceMeasureCategory> list, Long id) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getId().equals(id)) {
				return i;
			}
		}
		return -1;
	}

	private void swapAndReassignCategories(List<ChoiceMeasureCategory> list, int index1, int index2) {
		// Swap positions in list
		ChoiceMeasureCategory temp = list.get(index1);
		list.set(index1, list.get(index2));
		list.set(index2, temp);

		// Reassign sortOrder based on new positions
		for (int i = 0; i < list.size(); i++) {
			list.get(i).setSortOrder(i + 1);
			categoryService.save(list.get(i));
		}
	}

	// Helper methods for measures
	private int findMeasureIndex(List<ChoiceMeasure> list, Long id) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getId().equals(id)) {
				return i;
			}
		}
		return -1;
	}

	private void swapAndReassignMeasures(List<ChoiceMeasure> list, int index1, int index2) {
		// Swap positions in list
		ChoiceMeasure temp = list.get(index1);
		list.set(index1, list.get(index2));
		list.set(index2, temp);

		// Reassign sortOrder based on new positions
		for (int i = 0; i < list.size(); i++) {
			list.get(i).setSortOrder(i + 1);
			measuresService.save(list.get(i));
		}
	}
}
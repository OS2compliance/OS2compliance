package dk.digitalidentity.controller.mvc.Assets;

import dk.digitalidentity.Constants;
import dk.digitalidentity.model.entity.ChoiceMeasure;
import dk.digitalidentity.model.entity.ChoiceMeasureCategory;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.security.annotations.sections.RequireConfiguration;
import dk.digitalidentity.service.ChoiceMeasureCategoryService;
import dk.digitalidentity.service.ChoiceMeasuresService;
import dk.digitalidentity.service.ChoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequireConfiguration
@RequestMapping("assets/measures/schema")
@RequiredArgsConstructor
public class MeasuresSchemaController {
	private final ChoiceMeasureCategoryService categoryService;
	private final ChoiceMeasuresService measuresService;
	private final ChoiceService choiceService;


	@GetMapping
	public String schema() {
		return "assets/measures/schema";
	}

	record CategoryDTO(Long id, String name, Integer sortOrder, List<MeasureDTO> measures) {}
	record MeasureDTO(Long id, String identifier, String name, Boolean multiSelect, Integer sortOrder, boolean isFirst, boolean isLast) {}

	@GetMapping("fragment")
	@Transactional
	public String fragment(final Model model) {
		List<ChoiceMeasureCategory> categories = categoryService.findAll().stream()
				.sorted(Comparator.comparing(ChoiceMeasureCategory::getSortOrder))
				.toList();

		List<CategoryDTO> categoryDTOs = categories.stream()
				.map(cat -> {
					List<ChoiceMeasure> sortedMeasures = cat.getMeasures().stream()
							.filter(m -> !m.getDeleted())
							.sorted(Comparator.comparing(ChoiceMeasure::getSortOrder))
							.collect(Collectors.toList());

					List<MeasureDTO> measures = new ArrayList<>();
					for (int i = 0; i < sortedMeasures.size(); i++) {
						ChoiceMeasure m = sortedMeasures.get(i);
						boolean isFirst = (i == 0);
						boolean isLast = (i == sortedMeasures.size() - 1);
						measures.add(new MeasureDTO(
								m.getId(),
								m.getIdentifier(),
								m.getName(),
								m.getMultiSelect(),
								m.getSortOrder(),
								isFirst,
								isLast
						));
					}

					return new CategoryDTO(
							cat.getId(),
							cat.getName(),
							cat.getSortOrder(),
							measures
					);
				})
				.collect(Collectors.toList());

		model.addAttribute("categories", categoryDTOs);
		model.addAttribute("minCategorySortOrder", categories.isEmpty() ? 0 : categories.get(0).getSortOrder());
		model.addAttribute("maxCategorySortOrder", categories.isEmpty() ? 0 : categories.get(categories.size() - 1).getSortOrder());

		return "assets/measures/fragments/template :: measuresTemplateFragment";
	}

	// Category endpoints
	record CategoryFormDTO(Long id, String name) {}

	@GetMapping("category/form")
	public String categoryForm(final Model model, @RequestParam(name = "id", required = false) final Long id) {
		if (id == null) {
			model.addAttribute("category", new CategoryFormDTO(null, ""));
			model.addAttribute("formId", "createCategoryForm");
			model.addAttribute("formTitle", "Ny kategori");
		} else {
			final ChoiceMeasureCategory category = categoryService.findById(id)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
			model.addAttribute("category", new CategoryFormDTO(category.getId(), category.getName()));
			model.addAttribute("formId", "editCategoryForm");
			model.addAttribute("formTitle", "Rediger kategori");
		}
		return "assets/measures/fragments/categoryForm :: categoryForm";
	}

	@PostMapping("category/form")
	@Transactional
	public String categoryFormPost(@ModelAttribute final CategoryFormDTO dto, final Model model) {
		// Validation
		if (!StringUtils.hasLength(dto.name) || dto.name.length() < 2) {
			model.addAttribute("error", "Kategorinavn er påkrævet og skal være mindst 2 tegn");
			model.addAttribute("category", dto);
			model.addAttribute("formId", dto.id == null ? "createCategoryForm" : "editCategoryForm");
			model.addAttribute("formTitle", dto.id == null ? "Ny kategori" : "Rediger kategori");
			return "assets/measures/fragments/categoryForm :: categoryForm";
		}

		// Check for duplicate name (excluding current if editing)
		boolean nameExists = categoryService.findAll().stream()
				.filter(c -> !c.getId().equals(dto.id))
				.anyMatch(c -> c.getName().equalsIgnoreCase(dto.name));

		if (nameExists) {
			model.addAttribute("error", "En kategori med dette navn eksisterer allerede");
			model.addAttribute("category", dto);
			model.addAttribute("formId", dto.id == null ? "createCategoryForm" : "editCategoryForm");
			model.addAttribute("formTitle", dto.id == null ? "Ny kategori" : "Rediger kategori");
			return "assets/measures/fragments/categoryForm :: categoryForm";
		}

		if (dto.id != null) {
			ChoiceMeasureCategory category = categoryService.findById(dto.id)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
			category.setName(dto.name);
			categoryService.save(category);
		} else {
			Integer maxSortOrder = categoryService.findAll().stream()
					.map(ChoiceMeasureCategory::getSortOrder)
					.max(Integer::compareTo)
					.orElse(0);

			ChoiceMeasureCategory category = new ChoiceMeasureCategory();
			category.setName(dto.name);
			category.setSortOrder(maxSortOrder + 10);
			category.setDeleted(false);
			categoryService.save(category);
		}

		return "redirect:/assets/measures/schema";
	}

	// Measure endpoints
	record MeasureFormDTO(Long id, Long categoryId, String name, Boolean multiSelect, List<Long> valueIds) {}

	@GetMapping("measure/form")
	public String measureForm(final Model model, @RequestParam(name = "id", required = false) final Long id) {
		List<ChoiceMeasureCategory> categories = categoryService.findAll().stream()
				.sorted(Comparator.comparing(ChoiceMeasureCategory::getSortOrder))
				.collect(Collectors.toList());
		model.addAttribute("categories", categories);

		Set<String> identifiers = Arrays.stream(Constants.CHOICE_MEASURE_VALUE_IDENTIFIERS.split(","))
				.collect(Collectors.toSet());
		List<ChoiceValue> availableValues = choiceService.getValues(identifiers);
		model.addAttribute("availableValues", availableValues);

		if (id == null) {
			model.addAttribute("measure", new MeasureFormDTO(null, null, "", false, new ArrayList<>()));
			model.addAttribute("formId", "createMeasureForm");
			model.addAttribute("formTitle", "Nyt spørgsmål");
		} else {
			final ChoiceMeasure measure = measuresService.findById(id)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

			List<Long> selectedValueIds = measure.getValues().stream()
					.map(ChoiceValue::getId)
					.collect(Collectors.toList());

			model.addAttribute("measure", new MeasureFormDTO(
					measure.getId(),
					measure.getCategory().getId(),
					measure.getName(),
					measure.getMultiSelect(),
					selectedValueIds
			));

			model.addAttribute("formId", "editMeasureForm");
			model.addAttribute("formTitle", "Rediger spørgsmål");
		}
		return "assets/measures/fragments/measureForm :: measureForm";
	}
	@PostMapping("measure/form")
	@Transactional
	public String measureFormPost(@ModelAttribute final MeasureFormDTO dto, final Model model) {
		// Prepare model for potential error return
		List<ChoiceMeasureCategory> categories = categoryService.findAll().stream()
				.sorted(Comparator.comparing(ChoiceMeasureCategory::getSortOrder).thenComparing(ChoiceMeasureCategory::getId))
				.collect(Collectors.toList());

		Set<String> identifiers = Arrays.stream(Constants.CHOICE_MEASURE_VALUE_IDENTIFIERS.split(","))
				.collect(Collectors.toSet());
		List<ChoiceValue> availableValues = choiceService.getValues(identifiers);

		// Validation
		if (!StringUtils.hasLength(dto.name) || dto.name.length() < 3) {
			model.addAttribute("error", "Spørgsmål er påkrævet og skal være mindst 3 tegn");
			model.addAttribute("measure", dto);
			model.addAttribute("categories", categories);
			model.addAttribute("formId", dto.id == null ? "createMeasureForm" : "editMeasureForm");
			model.addAttribute("formTitle", dto.id == null ? "Nyt spørgsmål" : "Rediger spørgsmål");
			return "assets/measures/fragments/measureForm :: measureForm";
		}

		if (dto.categoryId == null) {
			model.addAttribute("error", "Kategori er påkrævet");
			model.addAttribute("measure", dto);
			model.addAttribute("categories", categories);
			model.addAttribute("formId", dto.id == null ? "createMeasureForm" : "editMeasureForm");
			model.addAttribute("formTitle", dto.id == null ? "Nyt spørgsmål" : "Rediger spørgsmål");
			return "assets/measures/fragments/measureForm :: measureForm";
		}

		ChoiceMeasureCategory category = categoryService.findById(dto.categoryId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		List<ChoiceValue> selectedValues = new ArrayList<>();
		if (dto.valueIds != null && !dto.valueIds.isEmpty()) {
			selectedValues = availableValues.stream()
					.filter(value -> dto.valueIds.contains(value.getId()))
					.collect(Collectors.toList());
		}

		if (dto.id != null) {
			ChoiceMeasure measure = measuresService.findById(dto.id)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
			measure.setCategory(category);
			measure.setName(dto.name);
			measure.setMultiSelect(dto.multiSelect != null ? dto.multiSelect : false);
			measure.setValues(selectedValues);
			measuresService.save(measure);
		} else {
			Integer maxSortOrder = category.getMeasures().stream()
					.filter(m -> !m.getDeleted())
					.map(ChoiceMeasure::getSortOrder)
					.max(Integer::compareTo)
					.orElse(0);

			ChoiceMeasure measure = new ChoiceMeasure();
			measure.setCategory(category);
			measure.setIdentifier(UUID.randomUUID().toString());
			measure.setName(dto.name);
			measure.setMultiSelect(dto.multiSelect != null ? dto.multiSelect : false);
			measure.setSortOrder(maxSortOrder + 10);
			measure.setDeleted(false);
			measure.setValues(selectedValues);
			measuresService.save(measure);
		}

		return "redirect:/assets/measures/schema";
	}
}
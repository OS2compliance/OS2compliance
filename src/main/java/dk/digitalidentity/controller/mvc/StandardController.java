package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.StandardSectionDao;
import dk.digitalidentity.dao.StandardTemplateDao;
import dk.digitalidentity.dao.StandardTemplateSectionDao;
import dk.digitalidentity.model.dto.RelatedDTO;
import dk.digitalidentity.model.dto.enums.AllowedAction;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.StandardTemplate;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.StandardSectionStatus;
import dk.digitalidentity.security.annotations.crud.RequireCreateAll;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.crud.RequireUpdateAll;
import dk.digitalidentity.security.annotations.sections.RequireStandard;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.StandardSectionService;
import dk.digitalidentity.service.StandardsService;
import dk.digitalidentity.service.SupportingStandardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("standards")
@RequireStandard
@RequiredArgsConstructor
public class StandardController {
    private final StandardsService standardsService;
    private final RelationService relationService;
    private final StandardSectionDao standardSectionDao;
    private final StandardTemplateDao standardTemplateDao;
    private final SupportingStandardService supportingStandardService;
	private final StandardTemplateSectionDao standardTemplateSectionDao;
	private final StandardSectionService standardSectionService;

	record StandardSectionDTO(StandardSection standardSection,
                              List<Relatable> relatedDocuments,
                              List<Relatable> relatedSections) {}
    record StandardTemplateSectionDTO(StandardTemplateSection standardTemplateSection,
                                      List<StandardSectionDTO> standardSectionDTOs) {}

	@RequireReadOwnerOnly
    @Transactional
    @GetMapping("section/{sectionId}")
    public String lookup(@PathVariable final Long sectionId) {
        final StandardSection section = standardSectionDao.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (standardsService.findStandardTemplateIdentifier(section).equals("iso27001")) {
            // Find out which topic
            switch (standardsService.findTopSectionNumber(section)) {
                case "4", "5", "6", "7" -> {
                    return "redirect:standards/plan";
                }
                case "8" -> {
                    return "redirect:standards/do";
                }
                case "10" -> {
                    return "redirect:standards/act";
                }
                case "9" -> {
                    return "redirect:standards/check";
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

	@RequireReadOwnerOnly
    @Transactional
    @GetMapping("plan")
    public String planPage(final Model model) {
        final List<StandardTemplateSectionDTO> sections = new ArrayList<>();
        sections.add(standardSection("4"));
        sections.add(standardSection("5"));
        sections.add(standardSection("6"));
        sections.add(standardSection("7"));

        model.addAttribute("sectionName", "plan");
        model.addAttribute("sections", sections);
        return "standards/iso27001";
    }

	@RequireReadOwnerOnly
    @Transactional
    @GetMapping("act")
    public String actPage(final Model model) {
        final List<StandardTemplateSectionDTO> sections = new ArrayList<>();
        sections.add(standardSection("10"));

        model.addAttribute("sectionName", "act");
        model.addAttribute("sections", sections);
        return "standards/iso27001";
    }

	@RequireReadOwnerOnly
    @Transactional
    @GetMapping("do")
    public String doPage(final Model model) {
        final List<StandardTemplateSectionDTO> sections = new ArrayList<>();
        sections.add(standardSection("8"));

        model.addAttribute("sectionName", "do");
        model.addAttribute("sections", sections);
        return "standards/iso27001";
    }

	@RequireReadOwnerOnly
    @Transactional
    @GetMapping("check")
    public String checkPage(final Model model) {
        final List<StandardTemplateSectionDTO> sections = new ArrayList<>();
        sections.add(standardSection("9"));

        model.addAttribute("sectionName", "check");
        model.addAttribute("sections", sections);
        return "standards/iso27001";
    }

    record StandardTemplateListDTO(String identifier, String name, String compliance, Set<AllowedAction> allowedActions, Map<StandardSectionStatus, Integer> progressBarValues, int totalCount) {}
	@RequireReadOwnerOnly
    @Transactional
    @GetMapping
    public String supportingPage(final Model model) {
        final List<StandardTemplateListDTO> templates = new ArrayList<>();
		Set<AllowedAction> allowedActions = supportingStandardService.calculateAllowedActions();
        for (final StandardTemplate standardTemplate : standardTemplateDao.findAllBySupportingIsTrue()) {
            final List<StandardTemplateSection> collect = standardTemplateSectionDao.findSelectedChildSectionsByTemplate(standardTemplate);
			final double readyCounter = standardTemplateSectionDao.countByTemplateAndStatus(standardTemplate, StandardSectionStatus.READY);
            final double notRelevantCount = standardTemplateSectionDao.countByTemplateAndStatus(standardTemplate, StandardSectionStatus.NOT_RELEVANT);
            final double relevantCount = collect.size() - notRelevantCount;
            final DecimalFormat decimalFormat = new DecimalFormat("0.00");

			Map<StandardSectionStatus, Integer> progressBarValues = getProgressBarValues(standardTemplate);
			if (relevantCount == 0 ) {
                templates.add(new StandardTemplateListDTO(standardTemplate.getIdentifier(), standardTemplate.getName(), decimalFormat.format(100) + "%", allowedActions, progressBarValues, progressBarValues.values().stream().mapToInt(Integer::intValue).sum()));
            } else {
                final double compliance = collect.isEmpty() ? 0 : 100 * (readyCounter / relevantCount);
                templates.add(new StandardTemplateListDTO(standardTemplate.getIdentifier(), standardTemplate.getName(), decimalFormat.format(compliance) + "%",  allowedActions, progressBarValues, progressBarValues.values().stream().mapToInt(Integer::intValue).sum()));
            }
        }
        model.addAttribute("templates", templates);
        return "standards/supporting";
    }

	@RequireReadOwnerOnly
    @Transactional
    @GetMapping("supporting/{id}")
    public String supportingPage(final Model model, @PathVariable final String id, @RequestParam(required = false) final StandardSectionStatus status) {
        StandardTemplate template = supportingStandardService.lookup(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("template", template);
        model.addAttribute("relationMap", buildRelationsMap(template));
        model.addAttribute("isNSIS", template.getIdentifier().toLowerCase().startsWith("nsis"));
        model.addAttribute("standardTemplateSectionComparator", Comparator.comparing(StandardTemplateSection::getSortKey));
        model.addAttribute("statusFilter", status);
		Map<StandardSectionStatus, Integer> progressBarValues = getProgressBarValues(template);
		if (progressBarValues == null) {
			progressBarValues = Collections.emptyMap();
		}
		model.addAttribute("progressbarValue", progressBarValues);
		model.addAttribute("totalCount", progressBarValues.values().stream().mapToInt(Integer::intValue).sum());
		model.addAttribute("statusColors", Map.of(
				"READY", "bg-green-500",
				"IN_PROGRESS", "bg-blue-500",
				"NOT_STARTED", "bg-yellow-500",
				"NOT_RELEVANT", "bg-gray-500"
				));

		model.addAttribute("statusTranslations", Map.of(
				"READY", "Færdigt",
				"IN_PROGRESS", "I gang",
				"NOT_STARTED", "Ikke startet",
				"NOT_RELEVANT", "Ikke relevant"
		));
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        model.addAttribute("today", LocalDate.now().format(formatter));
        return "standards/supporting_view";
    }

	@RequireCreateAll
	@Transactional
	@PostMapping("/create")
	public String newStandard(@Valid @ModelAttribute final StandardTemplate standard) {
		standard.setIdentifier(standard.getIdentifier()	.replaceAll("[.,\\s-]", "_"));
		standard.setSupporting(true);
		standardTemplateDao.save(standard);
		return "redirect:/standards";
	}

	@RequireUpdateAll
	@Transactional
	@PostMapping("/update")
	public String updateStandard(@Valid @ModelAttribute final StandardTemplate standard) {
		StandardTemplate template = supportingStandardService.lookup(standard.getIdentifier())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		template.setName(standard.getName());
		standardTemplateDao.save(template);
		return "redirect:/standards";
	}

	@RequireCreateAll
	@GetMapping("/form")
	public String createStandardForm(final Model model) {
		model.addAttribute("action", "standards/create");
		model.addAttribute("standard", new StandardTemplate());
		model.addAttribute("formTitle", "Ny standard");
		model.addAttribute("formId", "standardCreateForm");
		model.addAttribute("edit", false);
		return "standards/form";
	}

	@RequireUpdateAll
	@GetMapping("/form/{id}")
	public String editStandardForm(final Model model, @PathVariable final String id) {
		StandardTemplate template = supportingStandardService.lookup(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
		model.addAttribute("action", "standards/update");
		model.addAttribute("standard", template);
		model.addAttribute("formTitle", "Rediger standard");
		model.addAttribute("formId", "standardCreateForm");
		model.addAttribute("edit", true);

		return "standards/form";
	}

	@RequireCreateAll
	@GetMapping("/section/form/{id}")
	public String sectionForm(final Model model, @PathVariable(name = "id") final String id) {
		StandardTemplate template = supportingStandardService.lookup(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
		model.addAttribute("standard", template);
		model.addAttribute("action", "/standards/sections/create/" + id);
		model.addAttribute("section", new StandardSection());
		model.addAttribute("formTitle", "Ny krav");
		model.addAttribute("formId", "sectionCreateForm");

		return "standards/sections/create_section_form";
	}

	@RequireCreateAll
	@GetMapping("/section/header/form/{id}")
	public String createHeaderForm(final Model model, @PathVariable(name = "id") final String id) {
		StandardTemplate template = supportingStandardService.lookup(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		model.addAttribute("standard", template);
		model.addAttribute("action", "/standards/headers/create/" + id);
		model.addAttribute("header", new StandardTemplateSection());
		model.addAttribute("formTitle", "Ny gruppe");
		model.addAttribute("formId", "headerForm");

		return "standards/sections/create_header_form";
	}

	@RequireUpdateAll
	@GetMapping("/section/header/form/{id}/{headerIdentifier}")
	public String editHeaderForm(final Model model, @PathVariable(name = "id") final String headerIdentifier, @PathVariable(name = "headerIdentifier") final String id) {
		StandardTemplate template = supportingStandardService.lookup(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		StandardTemplateSection header = standardTemplateSectionDao.findById(headerIdentifier)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		model.addAttribute("header", header);
		model.addAttribute("standard", template);
		model.addAttribute("action", "/standards/headers/update/" + id);
		model.addAttribute("formTitle", "Ny gruppe");
		model.addAttribute("formId", "headerForm");

		return "standards/sections/create_header_form";
	}

	@RequireUpdateAll
	@Transactional
	@PostMapping("/headers/update/{identifier}")
	public String editHeader(@Valid @ModelAttribute final StandardTemplateSection standardTemplateSection, @PathVariable(name = "identifier") final String id, RedirectAttributes redirectAttributes, BindingResult result) {

		StandardTemplate template = supportingStandardService.lookup(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		StandardTemplateSection header = standardTemplateSectionDao.findByStandardTemplate(template).stream()
				.filter(section -> section.getParent() == null)
				.findAny()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));

		String oldSection = header.getSection().trim();
		String newSection = standardTemplateSection.getSection().trim();

		if (!Objects.equals(oldSection, newSection)) {
			boolean existsAlready = template.getStandardTemplateSections().stream().anyMatch(section -> section.getSection().equals(standardTemplateSection.getSection()));

			if (existsAlready) {
				result.rejectValue("section", "section.duplicate", "Du skal angive et unikt sektionsnummer");
				redirectAttributes.addFlashAttribute("headerModalError", result.getFieldErrors("section"));
				return "redirect:/standards/supporting/" + id;
			}

			String newSectionPrefix = newSection + ".";

			header.setSection(newSection);
			header.setSortKey(Integer.parseInt(newSection.replace(".", "")));

			List<StandardTemplateSection> children = header.getChildren().stream()
					.sorted(Comparator.comparing(StandardTemplateSection::getSortKey).reversed())
					.collect(Collectors.toList());
			for (StandardTemplateSection child : children) {
				String childSection = child.getSection().trim();
				String[] split = childSection.split("\\.");
				child.setSection(newSectionPrefix + split[split.length - 1]);
				child.setSortKey(Integer.parseInt(child.getSection().replace(".", "").replaceAll("[^0-9]", "")));
			}

			standardTemplateSectionDao.saveAll(children);
			standardTemplateSectionDao.save(header);
		}

		if (!Objects.equals(header.getDescription(), standardTemplateSection.getDescription())) {
			header.setDescription(standardTemplateSection.getDescription());
			standardTemplateSectionDao.save(header);
		}

		return "redirect:/standards/supporting/" + id;
	}

	@RequireCreateAll
	@Transactional
	@PostMapping("/sections/create/{identifier}")
	public String createSection(@Valid @ModelAttribute final StandardSection standardSection, @PathVariable(name = "identifier") final String identifier) {
		StandardTemplateSection parentsTemplateSection = standardSection.getTemplateSection();
		Set<StandardTemplateSection> existingChildren = parentsTemplateSection.getChildren();

		String version = getHighestVersionNumber(parentsTemplateSection.getSection(), existingChildren);
		String sectionPrefix = parentsTemplateSection.getSection();
		String name = version + " " + standardSection.getName();
		String templateSection = sectionPrefix + "." + version;
		standardSection.setSelected(true);
		standardSection.setStatus(StandardSectionStatus.IN_PROGRESS);
		String standardTemplateSectionIdentifier = getHighestVersionNumberBasedOnIds(parentsTemplateSection.getSection(), existingChildren);
		StandardTemplateSection newSection = new StandardTemplateSection();
		newSection.setIdentifier(standardTemplateSectionIdentifier);
		newSection.setSection(version);
		newSection.setDescription(standardSection.getName());
		newSection.setParent(parentsTemplateSection);
		newSection.setSortKey(Integer.parseInt(templateSection.replace(".", "").replaceAll("[^0-9]", "")));

		StandardTemplateSection save = standardTemplateSectionDao.save(newSection);

		standardSection.setName(name);
		standardSection.setTemplateSection(save);
		standardSectionService.save(standardSection);

		return "redirect:/standards/supporting/" + identifier;
	}

	@RequireCreateAll
	@Transactional
	@PostMapping("/headers/create/{identifier}")
	public String createHeader(@Valid @ModelAttribute final StandardTemplateSection standardTemplateSection, @PathVariable(name = "identifier") final String id, RedirectAttributes redirectAttributes, BindingResult result) {
		StandardTemplate template = supportingStandardService.lookup(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		boolean existsAlready = template.getStandardTemplateSections().stream().anyMatch(section -> section.getSection().equals(standardTemplateSection.getSection()));

		if (existsAlready) {
			result.rejectValue("section", "section.duplicate", "Du skal angive et unikt sektionsnummer");
			redirectAttributes.addFlashAttribute("headerModalError", result.getFieldErrors("section"));
			return "redirect:/standards/supporting/" + id;
		}
		String identifier = id.toLowerCase() + "_" + standardTemplateSection.getSection();
		String sortKey = standardTemplateSection.getSection().replace(".", "").replaceAll("[^0-9]", "");
		standardTemplateSection.setSortKey(Integer.parseInt(sortKey));
		standardTemplateSection.setIdentifier(identifier);
		standardTemplateSection.setStandardTemplate(template);
		standardTemplateSectionDao.save(standardTemplateSection);
		return "redirect:/standards/supporting/" + id;
	}

    private Map<Long, List<RelatedDTO>> buildRelationsMap(final StandardTemplate template) {
        final Map<Long, List<RelatedDTO>> result = new HashMap<>();

        for (final StandardTemplateSection standardTemplateSection : template.getStandardTemplateSections()) {
            for (final StandardTemplateSection child : standardTemplateSection.getChildren()) {
                result.put(child.getStandardSection().getId(), relationService.findRelationsAsListDTO(child.getStandardSection(), false));
            }
        }
        return result;
    }

    private StandardTemplateSectionDTO standardSection(final String topic) {
        final List<StandardSection> standardSections = standardsService.getSectionsForTopic(topic);
        final StandardTemplateSection parent = standardSections.stream().filter(s -> s.getTemplateSection().getParent() != null).findFirst()
            .map(s -> s.getTemplateSection().getParent())
            .orElseThrow(() -> new IllegalStateException("No parent found for entry"));
        final List<StandardSectionDTO> standardSectionDTOs = standardSections.stream()
            .map(s -> {
                final List<Relatable> relatedTo = relationService.findAllRelatedTo(s);
                return new StandardSectionDTO(s,
                    filterRelations(relatedTo, RelationType.DOCUMENT),
                    filterRelations(relatedTo, RelationType.STANDARD_SECTION));
            })
				.toList();
        return new StandardTemplateSectionDTO(parent, standardSectionDTOs);
    }

    private static List<Relatable> filterRelations(final List<Relatable> relatables, final RelationType relationType) {
        return relatables.stream()
            .filter(r -> r.getRelationType().equals(relationType))
            .toList();
    }

	private String getHighestVersionNumber(String parentSection, Set<StandardTemplateSection> allSections) {
		String prefix = parentSection + ".";

		int temp = 0;
		for (StandardTemplateSection allSection : allSections) {
			String[] split = allSection.getSection().split("\\.");
			int value = Integer.parseInt(split[split.length - 1]);
			if (value > temp) {
				temp = value;
			}
		}
		return prefix + (temp == 0 ? 1 : (temp + 1));
	}

	private String getHighestVersionNumberBasedOnIds(String parentSection, Set<StandardTemplateSection> allSections) {
		String prefix = parentSection + ".";
		int max = 0;
		// Matching the last number, to figure out the highest number we can take
		Pattern numberPattern = Pattern.compile("(\\d+)$");

		for (StandardTemplateSection section : allSections) {
			String identifier = section.getIdentifier();

			Matcher matcher = numberPattern.matcher(identifier);
			if (matcher.find()) {
				try {
					int number = Integer.parseInt(matcher.group(1));
					if (number > max) {
						max = number;
					}
				} catch (NumberFormatException ignored) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
				}
			}
		}

		return prefix + (max + 1);
	}

	private Map<StandardSectionStatus, Integer> getProgressBarValues(final StandardTemplate template) {
		Map<StandardSectionStatus, Integer> result = new HashMap<>();
		for (StandardTemplateSection standardTemplateSection : template.getStandardTemplateSections()) {
			standardTemplateSection.getChildren().forEach(child -> {
				result.put(child.getStandardSection().getStatus(), result.getOrDefault(child.getStandardSection().getStatus(), 0) + 1);
			});
		}
		return result;
	}

}

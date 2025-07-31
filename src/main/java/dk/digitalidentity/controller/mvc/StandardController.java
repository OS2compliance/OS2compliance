package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.StandardSectionDao;
import dk.digitalidentity.dao.StandardTemplateDao;
import dk.digitalidentity.dao.StandardTemplateSectionDao;
import dk.digitalidentity.model.dto.RelatedDTO;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.StandardTemplate;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.StandardSectionStatus;
import dk.digitalidentity.security.RequireUser;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("standards")
@RequireUser
@RequiredArgsConstructor
public class StandardController {
	// TODO: Refactor to use service only instead of direct dao!
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

    @Transactional
    @GetMapping("act")
    public String actPage(final Model model) {
        final List<StandardTemplateSectionDTO> sections = new ArrayList<>();
        sections.add(standardSection("10"));

        model.addAttribute("sectionName", "act");
        model.addAttribute("sections", sections);
        return "standards/iso27001";
    }

    @Transactional
    @GetMapping("do")
    public String doPage(final Model model) {
        final List<StandardTemplateSectionDTO> sections = new ArrayList<>();
        sections.add(standardSection("8"));

        model.addAttribute("sectionName", "do");
        model.addAttribute("sections", sections);
        return "standards/iso27001";
    }

    @Transactional
    @GetMapping("check")
    public String checkPage(final Model model) {
        final List<StandardTemplateSectionDTO> sections = new ArrayList<>();
        sections.add(standardSection("9"));

        model.addAttribute("sectionName", "check");
        model.addAttribute("sections", sections);
        return "standards/iso27001";
    }

    record StandardTemplateListDTO(String identifier, String name, String compliance) {}
    @Transactional
    @GetMapping
    public String supportingPage(final Model model) {
        final List<StandardTemplateListDTO> templates = new ArrayList<>();
        for (final StandardTemplate standardTemplate : standardTemplateDao.findAll().stream().filter(StandardTemplate::isSupporting).toList()) {
            final List<StandardTemplateSection> collect = standardTemplate.getStandardTemplateSections().stream()
                .flatMap(s -> s.getChildren().stream())
                .filter(s -> s.getStandardSection().isSelected())
                .toList();
            final double readyCounter = collect.stream().filter(s -> Objects.equals(s.getStandardSection().getStatus(), StandardSectionStatus.READY)).count();
            final double notRelevantCount = collect.stream().filter(s -> Objects.equals(s.getStandardSection().getStatus(), StandardSectionStatus.NOT_RELEVANT)).count();
            final double relevantCount = collect.size() - notRelevantCount;
            final DecimalFormat decimalFormat = new DecimalFormat("0.00");

            if (relevantCount == 0 ) {
                templates.add(new StandardTemplateListDTO(standardTemplate.getIdentifier(), standardTemplate.getName(), decimalFormat.format(100) + "%"));
            } else {
                final double compliance = collect.isEmpty() ? 0 : 100 * (readyCounter / relevantCount);
                templates.add(new StandardTemplateListDTO(standardTemplate.getIdentifier(), standardTemplate.getName(), decimalFormat.format(compliance) + "%"));
            }
        }
        model.addAttribute("templates", templates);
        return "standards/supporting";
    }

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

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        model.addAttribute("today", LocalDate.now().format(formatter));
        return "standards/supporting_view";
    }

	@Transactional
	@PostMapping("/create")
	public String newStandard(@Valid @ModelAttribute final StandardTemplate standard) {
		standard.setIdentifier(standard.getIdentifier()	.replaceAll("[.,\\s-]", "_"));
		standard.setSupporting(true);
		standardTemplateDao.save(standard);
		return "redirect:/standards";
	}

	@GetMapping("/form")
	public String standardForm(final Model model) {
		model.addAttribute("action", "standards/create");
		model.addAttribute("standard", new StandardTemplate());
		model.addAttribute("formTitle", "Ny standard");
		model.addAttribute("formId", "standardCreateForm");

		return "standards/form";
	}

	@GetMapping("/section/form/{id}")
	public String sectionForm(final Model model, @PathVariable(name = "id") final String id) {
		StandardTemplate template = supportingStandardService.lookup(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		model.addAttribute("standard", template);
		model.addAttribute("action", "/standards/sections/create/" + id);
		model.addAttribute("section", new StandardSection());
		model.addAttribute("formTitle", "Ny krav");
		model.addAttribute("formId", "sectionCreateForm");

		return "standards/sections/create_section_form";
	}

	@GetMapping("/section/header/form/{id}")
	public String headerForm(final Model model, @PathVariable(name = "id") final String id) {
		StandardTemplate template = supportingStandardService.lookup(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		model.addAttribute("standard", template);
		model.addAttribute("action", "/standards/headers/create/" + id);
		model.addAttribute("header", new StandardTemplateSection());
		model.addAttribute("formTitle", "Ny gruppe");
		model.addAttribute("formId", "headerForm");

		return "standards/sections/create_header_form";
	}

	@Transactional
	@PostMapping("/sections/create/{identifier}")
	public String createSection(@Valid @ModelAttribute final StandardSection standardSection, @PathVariable(name = "identifier") final String identifier) {
		StandardTemplateSection parentsTemplateSection = standardSection.getTemplateSection();
		Set<StandardTemplateSection> existingChildren = parentsTemplateSection.getChildren();

		// TODO: Versioning is cooked, when a section already exists we need to increment the counter, if there is no underlying section already we take nr 1.
		// TODO: When deleting we need to make sure all the other ones are updated properly so that we do not end up with 11.3, 11.5, 11.6
		String version = getHighestVersionNumber(parentsTemplateSection.getSection(), existingChildren);
		log.info("version: " + version);
		String sectionPrefix = parentsTemplateSection.getSection();
		String name = sectionPrefix  + "."  + version + " " + standardSection.getName();
		String templateSection = sectionPrefix + "." + version;

		String standardTemplateSectionIdentifier = getHighestVersionNumberBasedOnIds(parentsTemplateSection.getSection(), existingChildren);
		log.info("standardTemplateSectionIdentifier: " + standardTemplateSectionIdentifier);
		StandardTemplateSection newSection = new StandardTemplateSection();
		newSection.setIdentifier(standardTemplateSectionIdentifier);
		newSection.setSection(version);
		newSection.setDescription(standardSection.getName());
		newSection.setParent(parentsTemplateSection);
		newSection.setSortKey(Integer.parseInt(templateSection.replace(".", "")));

		StandardTemplateSection save = standardTemplateSectionDao.save(newSection);

		standardSection.setName(name);
		standardSection.setTemplateSection(save);
		standardSectionService.save(standardSection);

		return "redirect:/standards/supporting/" + identifier;
	}

	// TODO: Validate that no version already exists beforehand, make sure we error on it and that the user gets to see the error
	@Transactional
	@PostMapping("/headers/create/{identifier}")
	public String createHeader(@Valid @ModelAttribute final StandardTemplateSection standardTemplateSection, @PathVariable(name = "identifier") final String id, RedirectAttributes redirectAttributes, BindingResult result) {
		StandardTemplate template = supportingStandardService.lookup(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		// TODO: There has to be a different way to properly display the errors
		boolean existsAlready = template.getStandardTemplateSections().stream().anyMatch(section -> section.getSection().equals(standardTemplateSection.getSection()));

		if (existsAlready) {
			result.rejectValue("section", "section.duplicate", "Du skal angive et unikt sektionsnummer");
			redirectAttributes.addFlashAttribute("headerModalError", result.getFieldErrors("section"));
			return "redirect:/standards/supporting/" + id;
		}
		String identifier = id.toLowerCase() + "_" + standardTemplateSection.getSection();
		String sortKey = standardTemplateSection.getSection().replace(".", "");
		standardTemplateSection.setSortKey(Integer.parseInt(sortKey));
		standardTemplateSection.setIdentifier(identifier);
		standardTemplateSection.setStandardTemplate(template);
		standardTemplateSectionDao.save(standardTemplateSection);
		return "redirect:/standards/supporting/" + id;
	}

    private Map<Long, List<RelatedDTO>> buildRelationsMap(final StandardTemplate template) {
        final Map<Long, List<RelatedDTO>> result = new HashMap<>();

        for (final StandardTemplateSection standardTemplateSection : template.getStandardTemplateSections()) {
            for(final StandardTemplateSection child : standardTemplateSection.getChildren()) {
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
            .collect(Collectors.toList());
        return new StandardTemplateSectionDTO(parent, standardSectionDTOs);
    }

    private static List<Relatable> filterRelations(final List<Relatable> relatables, final RelationType relationType) {
        return relatables.stream()
            .filter(r -> r.getRelationType().equals(relationType))
            .collect(Collectors.toList());
    }


	// TODO: Make this one function and parameterize it?

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

		int temp = 0;
		for (StandardTemplateSection allSection : allSections) {
			String[] split = allSection.getIdentifier().split("\\.");
			int value = Integer.parseInt(split[split.length - 1]);
			if (value > temp) {
				temp = value;
			}
		}
		return prefix + (temp == 0 ? 1 : (temp + 1));
	}
}

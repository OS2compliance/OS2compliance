package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.StandardSectionDao;
import dk.digitalidentity.dao.StandardTemplateDao;
import dk.digitalidentity.model.dto.RelatedDTO;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.StandardTemplate;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.StandardSectionStatus;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.StandardsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("standards")
@RequireUser
@RequiredArgsConstructor
public class StandardController {
    private final StandardsService standardsService;
    private final RelationService relationService;
    private final StandardSectionDao standardSectionDao;
    private final StandardTemplateDao standardTemplateDao;


    record StandardSectionDTO(StandardSection standardSection,
                              List<Relatable> relatedDocuments,
                              List<Relatable> relatedSections) {}
    record StandardTemplateSectionDTO(StandardTemplateSection standardTemplateSection,
                                      List<StandardSectionDTO> standardSectionDTOs) {}
    @GetMapping
    public String index() {
        return "standards/index";
    }


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
    @GetMapping("supporting")
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
        final StandardTemplate template = standardTemplateDao.findByIdentifier(id);
        if (template == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("template", template);
        model.addAttribute("relationMap", buildRelationsMap(template));
        model.addAttribute("isNSIS", template.getIdentifier().toLowerCase().startsWith("nsis"));
        model.addAttribute("standardTemplateSectionComparator", Comparator.comparing(StandardTemplateSection::getSortKey));
        model.addAttribute("statusFilter", status);

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        model.addAttribute("today", LocalDate.now().format(formatter));

        return "standards/supporting_view";
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

}

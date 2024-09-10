package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.security.RequireAdminstrator;
import dk.digitalidentity.service.CatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("catalogs")
@RequireAdminstrator
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogService threatCatalogService;

    @GetMapping
    public String riskList(final Model model) {
        final List<ThreatCatalog> catalogList = threatCatalogService.findAll();
        model.addAttribute("threatCatalogs", catalogList);
        model.addAttribute("inUse", catalogList.stream()
            .collect(Collectors.toMap(ThreatCatalog::getIdentifier, threatCatalogService::inUse)));
        return "catalogs/index";
    }

    @GetMapping("{identifier}")
    public String view(final Model model, @PathVariable final String identifier) {
        final ThreatCatalog threatCatalog = threatCatalogService.get(identifier)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("catalog", threatCatalog);
        return "catalogs/view";
    }

    @GetMapping("threatForm")
    public String threatForm(final Model model, @RequestParam(name = "catalogIdentifier") final String catalogIdentifier, @RequestParam(name = "identifier", required = false) final String identifier) {
        final ThreatCatalog catalog = threatCatalogService.get(catalogIdentifier)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("catalog", catalog);
        if (identifier == null) {
            model.addAttribute("threat", new ThreatCatalogThreat());
            model.addAttribute("formId", "createForm");
            model.addAttribute("formTitle", "Ny trussel");
        } else {
            final ThreatCatalogThreat threat = catalog.getThreats().stream()
                .filter(t -> identifier.equals(t.getIdentifier()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            model.addAttribute("threat", threat);
            model.addAttribute("formId", "editForm");
            model.addAttribute("formTitle", "Rediger trussel");
        }
        return "catalogs/threatForm";
    }

    @Transactional
    @PostMapping("threatForm")
    public String threatFormPost(@RequestParam(name = "catalogIdentifier") final String catalogIdentifier, @ModelAttribute final ThreatCatalogThreat threat) {
        final ThreatCatalog catalog = threatCatalogService.get(catalogIdentifier)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final ThreatCatalogThreat existingThreat = catalog.getThreats().stream()
            .filter(t -> threat.getIdentifier().equals(t.getIdentifier()))
            .findFirst()
            .orElseGet(() -> {
                // New threat, make sure to set the sort key to max
                final long currentMax = getMaxSortKeyInType(threat.getThreatType(), catalog);
                threat.setIdentifier(UUID.randomUUID().toString());
                threat.setThreatCatalog(catalog);
                threat.setSortKey(currentMax+1);
                return threatCatalogService.saveThreat(threat);
            });
        if (!existingThreat.getThreatType().equalsIgnoreCase(threat.getThreatType())) {
            // Ohh boy they changed category type, we need a new
            existingThreat.setSortKey(getMaxSortKeyInType(threat.getThreatType(), catalog) + 1);
        }
        existingThreat.setThreatType(threat.getThreatType());
        existingThreat.setDescription(threat.getDescription());
        return "redirect:/catalogs/" + catalogIdentifier;
    }

    private static long getMaxSortKeyInType(final String threatType, final ThreatCatalog catalog) {
        return catalog.getThreats().stream()
            .filter(t -> StringUtils.equalsIgnoreCase(t.getThreatType(), threatType))
            .mapToLong(ThreatCatalogThreat::getSortKey)
            .filter(Objects::nonNull)
            .max().orElse(0L);
    }

    @GetMapping("form")
    public String form(final Model model, @RequestParam(name = "id", required = false) final String id) {
        model.addAttribute("action", "catalogs/form");
        if (id == null) {
            model.addAttribute("catalog", new ThreatCatalog());
            model.addAttribute("formId", "createForm");
            model.addAttribute("formTitle", "Nyt trusselskatalog");
        } else {
            final ThreatCatalog catalog = threatCatalogService.get(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            model.addAttribute("catalog", catalog);
            model.addAttribute("formId", "editForm");
            model.addAttribute("formTitle", "Rediger trusselskatalog");
        }
        return "catalogs/form";
    }

    @GetMapping("copy")
    public String copyForm(final Model model, @RequestParam(name = "id") final String id) {
        final ThreatCatalog catalog = threatCatalogService.get(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("action", "catalogs/copy");
        model.addAttribute("catalog", catalog);
        model.addAttribute("formId", "copyForm");
        model.addAttribute("formTitle", "Kopier trusselskatalog");
        return "catalogs/form";
    }

    @Transactional
    @PostMapping("form")
    public String formPost(@ModelAttribute final ThreatCatalog catalog) {
        if (StringUtils.isNotEmpty(catalog.getIdentifier())) {
            final ThreatCatalog existingCatalog = threatCatalogService.get(catalog.getIdentifier())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            existingCatalog.setName(catalog.getName());
            existingCatalog.setHidden(catalog.isHidden());
        } else {
            catalog.setIdentifier(UUID.randomUUID().toString());
            threatCatalogService.save(catalog);
        }
        return "redirect:/catalogs";
    }

    @Transactional
    @PostMapping("copy")
    public String copyPost(@ModelAttribute final ThreatCatalog catalog) {
        threatCatalogService.copyToNew(catalog);
        return "redirect:/catalogs";
    }

}

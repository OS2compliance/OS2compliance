package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.CatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Controller
@RequestMapping("catalogs")
@RequireUser
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogService threatCatalogService;

    @GetMapping
    public String riskList(final Model model) {
        model.addAttribute("threatCatalogs", threatCatalogService.findAll());
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
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        existingThreat.setThreatType(threat.getThreatType());
        existingThreat.setDescription(threat.getDescription());
        return "redirect:/catalogs/" + catalogIdentifier;
    }

    @GetMapping("form")
    public String form(final Model model, @RequestParam(name = "id", required = false) final String id) {
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

    @Transactional
    @PostMapping("form")
    public String formPost(@ModelAttribute final ThreatCatalog catalog) {
        if (catalog.getIdentifier() != null) {
            final ThreatCatalog existingCatalog = threatCatalogService.get(catalog.getIdentifier())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            existingCatalog.setName(catalog.getName());
            existingCatalog.setHidden(catalog.isHidden());
        } else {
            threatCatalogService.save(catalog);
        }
        return "redirect:/catalogs";
    }

}

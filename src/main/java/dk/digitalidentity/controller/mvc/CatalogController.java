package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.entity.ThreatCatalog;
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
    public String formPost(@ModelAttribute final ThreatCatalog supplier) {
        return "redirect:/catalogs";
    }

}

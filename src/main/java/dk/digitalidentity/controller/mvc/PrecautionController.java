package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.entity.Precaution;
import dk.digitalidentity.security.RequireAdminstrator;
import dk.digitalidentity.service.PrecautionService;
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

import java.util.List;

@Slf4j
@Controller
@RequestMapping("precautions")
@RequireAdminstrator
@RequiredArgsConstructor
public class PrecautionController {
    private final PrecautionService precautionService;

    @GetMapping
    public String precautionList(final Model model) {
        final List<Precaution> precautions = precautionService.findAll();
        model.addAttribute("precautions", precautions);
        return "precautions/index";
    }

//    @GetMapping("{identifier}")
//    public String view(final Model model, @PathVariable final String identifier) {
//        final ThreatCatalog threatCatalog = threatCatalogService.get(identifier)
//            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
//        model.addAttribute("catalog", threatCatalog);
//        return "catalogs/view";
//    }
//
//    @GetMapping("threatForm")
//    public String threatForm(final Model model, @RequestParam(name = "catalogIdentifier") final String catalogIdentifier, @RequestParam(name = "identifier", required = false) final String identifier) {
//        final ThreatCatalog catalog = threatCatalogService.get(catalogIdentifier)
//            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
//        model.addAttribute("catalog", catalog);
//        if (identifier == null) {
//            model.addAttribute("threat", new ThreatCatalogThreat());
//            model.addAttribute("formId", "createForm");
//            model.addAttribute("formTitle", "Ny trussel");
//        } else {
//            final ThreatCatalogThreat threat = catalog.getThreats().stream()
//                .filter(t -> identifier.equals(t.getIdentifier()))
//                .findFirst()
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
//            model.addAttribute("threat", threat);
//            model.addAttribute("formId", "editForm");
//            model.addAttribute("formTitle", "Rediger trussel");
//        }
//        return "catalogs/threatForm";
//    }
//
//    @Transactional
//    @PostMapping("threatForm")
//    public String threatFormPost(@RequestParam(name = "catalogIdentifier") final String catalogIdentifier, @ModelAttribute final ThreatCatalogThreat threat) {
//        final ThreatCatalog catalog = threatCatalogService.get(catalogIdentifier)
//            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
//        final ThreatCatalogThreat existingThreat = catalog.getThreats().stream()
//            .filter(t -> threat.getIdentifier().equals(t.getIdentifier()))
//            .findFirst()
//            .orElseGet(() -> {
//                // New threat, make sure to set the sort key to max
//                final long currentMax = catalog.getThreats().stream()
//                    .filter(t -> StringUtils.equalsIgnoreCase(t.getThreatType(), threat.getThreatType()))
//                    .mapToLong(ThreatCatalogThreat::getSortKey)
//                    .filter(Objects::nonNull)
//                    .max().orElse(0L);
//                threat.setIdentifier(UUID.randomUUID().toString());
//                threat.setThreatCatalog(catalog);
//                threat.setSortKey(currentMax+1);
//                return threatCatalogService.saveThreat(threat);
//            });
//        existingThreat.setThreatType(threat.getThreatType());
//        existingThreat.setDescription(threat.getDescription());
//        return "redirect:/catalogs/" + catalogIdentifier;
//    }

    @GetMapping("form")
    public String form(final Model model, @RequestParam(name = "id", required = false) final Long id) {
        model.addAttribute("action", "precautions/form");
        if (id == null) {
            model.addAttribute("precaution", new Precaution());
            model.addAttribute("formId", "createForm");
            model.addAttribute("formTitle", "Ny foranstaltning");
        } else {
            final Precaution precaution = precautionService.get(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            model.addAttribute("precaution", precaution);
            model.addAttribute("formId", "editForm");
            model.addAttribute("formTitle", "Rediger foranstaltning");
        }
        return "precautions/form";
    }
//
//    @GetMapping("copy")
//    public String copyForm(final Model model, @RequestParam(name = "id") final String id) {
//        final ThreatCatalog catalog = threatCatalogService.get(id)
//            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
//        model.addAttribute("action", "catalogs/copy");
//        model.addAttribute("catalog", catalog);
//        model.addAttribute("formId", "copyForm");
//        model.addAttribute("formTitle", "Kopier trusselskatalog");
//        return "catalogs/form";
//    }

    @Transactional
    @PostMapping("form")
    public String formPost(@ModelAttribute final Precaution precaution) {
        if (precaution.getId() != null) {
            final Precaution existingPrecaution = precautionService.get(precaution.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            existingPrecaution.setName(precaution.getName());
            existingPrecaution.setDescription(precaution.getDescription());
            precautionService.save(existingPrecaution);
        } else {
            precautionService.save(precaution);
        }
        return "redirect:/precautions";
    }

//    @Transactional
//    @PostMapping("copy")
//    public String copyPost(@ModelAttribute final ThreatCatalog catalog) {
//        threatCatalogService.copyToNew(catalog);
//        return "redirect:/catalogs";
//    }

}

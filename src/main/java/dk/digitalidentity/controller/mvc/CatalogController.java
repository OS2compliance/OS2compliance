package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.CatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
        return "catalog/index";
    }

}

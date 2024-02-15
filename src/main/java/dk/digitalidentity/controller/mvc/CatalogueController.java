package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.CatalogueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("catalogues")
@RequireUser
@RequiredArgsConstructor
public class CatalogueController {
    private final CatalogueService threatCatalogueService;

    @GetMapping
    public String riskList(final Model model) {
        model.addAttribute("threatCatalogs", threatCatalogueService.findAll());
        return "catalogue/index";
    }

}

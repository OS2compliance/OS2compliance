package dk.digitalidentity.controller.mvc.Admin;

import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.security.RequireAdministrator;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.service.ChoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequireAdministrator
@RequestMapping("admin/choicelists")
@RequiredArgsConstructor
public class CustomChoiceListController {

    private final ChoiceService choiceService;

    @GetMapping()
    public String customChoiceListsIndex(Model model) {

        List<ChoiceList> customChoiceLists = choiceService.getAllCustomizableChoiceLists();
        model.addAttribute("choiceLists", customChoiceLists);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("isSuperuser", authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)));
        return "admin/custom_choice_lists";
    }
}

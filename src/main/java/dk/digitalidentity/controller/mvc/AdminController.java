package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.security.RequireAdminstrator;
import dk.digitalidentity.service.ResponsibleUserViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequireAdminstrator
@RequestMapping("admin")
@RequiredArgsConstructor
public class AdminController {
    private final ResponsibleUserViewService responsibleUserViewService;

    @GetMapping("inactive")
    public String inactiveResponsibleList(Model model) {
        model.addAttribute("users", responsibleUserViewService.findInactiveResponsibleUsers());
        return "admin/inactive_users";
    }
}

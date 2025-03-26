package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("dpia")
@RequireUser
@RequiredArgsConstructor
public class DPIAController {


    @GetMapping
    public String dpiaList(final Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("superuser", authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)));
        return "dpia/index";
    }
}

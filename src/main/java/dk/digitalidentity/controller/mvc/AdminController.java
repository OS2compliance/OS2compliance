package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.security.RequireAdminstrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequireAdminstrator
@RequestMapping("admin")
@RequiredArgsConstructor
public class AdminController {
    @GetMapping("inactive")
    public String inactiveResponsibleList() {
        return "admin/inactive_users";
    }
}

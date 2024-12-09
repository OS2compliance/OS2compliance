package dk.digitalidentity.controller.mvc.Assets;

import dk.digitalidentity.model.dto.RoleDTO;
import dk.digitalidentity.security.RequireSuperuser;
import dk.digitalidentity.security.RequireUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequireUser
@RequestMapping("asset/roles")
@RequiredArgsConstructor
public class RoleController {

    @GetMapping("create/{assetId}")
    public String createRole (final Model model, @PathVariable Long assetId) {
         model.addAttribute("role", new RoleDTO(null, "", assetId));
         return "assets/roles/editRoleModal";
    }

}

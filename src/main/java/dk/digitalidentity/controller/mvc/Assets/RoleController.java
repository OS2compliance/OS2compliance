package dk.digitalidentity.controller.mvc.Assets;

import dk.digitalidentity.mapping.RoleMapper;
import dk.digitalidentity.model.dto.RoleDTO;
import dk.digitalidentity.model.entity.Role;
import dk.digitalidentity.security.annotations.crud.RequireCreateAll;
import dk.digitalidentity.security.annotations.crud.RequireUpdateAll;
import dk.digitalidentity.security.annotations.sections.RequireAsset;
import dk.digitalidentity.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@Slf4j
@Controller
@RequireAsset
@RequestMapping("asset/roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;
    private final RoleMapper mapper;

    @RequireCreateAll
    @GetMapping("create/{assetId}")
    public String createRole(final Model model, @PathVariable Long assetId) {
        model.addAttribute("role", new RoleDTO(null, "", assetId, new ArrayList<>()));
        return "assets/roles/editRoleModal";
    }

    @RequireUpdateAll
    @GetMapping("edit/{assetId}/{roleId}")
    public String editRole(final Model model, @PathVariable Long assetId, @PathVariable Long roleId) {
        Role role = roleService.getRole(roleId)
            .orElseThrow();


        model.addAttribute("role", mapper.toDTO(role));
        return "assets/roles/editRoleModal";
    }

}

package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.dto.UserWithRoleDTO;
import dk.digitalidentity.security.RequireAdminstrator;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.management.relation.Role;
import java.util.stream.Collectors;

@Controller
@RequireAdminstrator
@RequestMapping("admin/users")
@RequiredArgsConstructor
public class UsersController {
    final private UserService userService;

    @GetMapping("all")
    public String inactiveResponsibleList(Model model) {
        model.addAttribute("allUsers", userService.getAll().stream().map(user -> UserWithRoleDTO.builder()
            .uuid(user.getUuid())
            .userId(user.getUserId())
            .name(user.getName())
            .accessRole(user.getRoles().contains(Roles.ADMINISTRATOR) ? Roles.ADMINISTRATOR : Roles.USER)
            .build()
        ).collect(Collectors.toSet()));
        model.addAttribute("user", UserWithRoleDTO.builder().build());
        return "users/index";
    }

}

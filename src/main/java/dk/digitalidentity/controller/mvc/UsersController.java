package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.dto.RoleOptionDTO;
import dk.digitalidentity.model.dto.UserWithRoleDTO;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.security.RequireAdminstrator;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequireAdminstrator
@RequestMapping("admin/users")
@RequiredArgsConstructor
public class UsersController {
    final private UserService userService;

    final private Map<String, RoleOptionDTO> roleOptions = Map.of(
        "user", new RoleOptionDTO("user", "Bruger"),
        "admin", new RoleOptionDTO("admin", "Administrator")
    );

    @GetMapping("all")
    public String allUsers(Model model) {
        //model for role options
        model.addAttribute("roleOptions", new ArrayList<RoleOptionDTO>(roleOptions.values()));

        //Model for list of users
        model.addAttribute("allUsers", userService.getAll().stream().map(user -> UserWithRoleDTO.builder()
            .uuid(user.getUuid())
            .userId(user.getUserId())
            .name(user.getName())
            .email(user.getEmail())
            .active(user.getActive())
            .accessRole(user.getRoles().contains(Roles.ADMINISTRATOR) ? "admin" : "user")
            .build()
        ).collect(Collectors.toSet()));

        //model for creating new users
        model.addAttribute("user", UserWithRoleDTO.builder().build());

        return "users/index";
    }

    @GetMapping("create")
    public String createUser(Model model) {
        //model for role options
        model.addAttribute("roleOptions", new ArrayList<RoleOptionDTO>(roleOptions.values()));

        //model for user being edited
        model.addAttribute("user", UserWithRoleDTO.builder().build());

        return "users/fragments/edit";
    }

    @GetMapping("edit/{id}")
    public String editUser(Model model, @PathVariable("id") final String id) {
        //model for role options
        model.addAttribute("roleOptions", new ArrayList<RoleOptionDTO>(roleOptions.values()));

        //model for user being edited
        User user = userService.findByUuid(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));


        model.addAttribute("user", UserWithRoleDTO.builder()
            .uuid(user.getUuid())
            .userId(user.getUserId())
            .name(user.getName())
            .email(user.getEmail())
            .active(user.getActive())
            .accessRole(user.getRoles().contains(Roles.ADMINISTRATOR) ? "admin" : "user")
            .build());

        return "users/fragments/edit";
    }

    @Transactional
    @PostMapping("create")
    public String createUser(@ModelAttribute UserWithRoleDTO user) {
        User fullUser = User.builder()
            .userId(user.getUserId())
            .name(user.getName())
            .active(true)
            .password("Test1234")
            .build();
        Set<String> roles = new HashSet<>();
        roles.add(Roles.USER);
        if (user.getAccessRole().equals("admin")) {
            roles.add(Roles.ADMINISTRATOR);
        }
        fullUser.setRoles(roles);
        userService.create(fullUser);
        return "redirect:/admin/users/all";

    }
}

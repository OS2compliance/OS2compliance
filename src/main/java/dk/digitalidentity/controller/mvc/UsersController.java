package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.dto.RoleOptionDTO;
import dk.digitalidentity.model.dto.UserWithRoleDTO;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.security.RequireAdminstrator;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequireAdminstrator
@RequestMapping("admin/users")
@RequiredArgsConstructor
public class UsersController {
    final private UserService userService;


    @GetMapping("all")
    public String allUsers(Model model) {
        //model for role options
        Map<String, RoleOptionDTO> roleOptions = new HashMap();
        roleOptions.put("user", new RoleOptionDTO("user", "Bruger"));
        roleOptions.put("admin", new RoleOptionDTO("admin", "Administrator"));
        model.addAttribute("roleOptions", new ArrayList<RoleOptionDTO>(roleOptions.values()));

        //Model for list of users
        model.addAttribute("allUsers", userService.getAll().stream().map(user -> UserWithRoleDTO.builder()
            .uuid(user.getUuid())
            .userId(user.getUserId())
            .name(user.getName())
            .accessRole(user.getRoles().contains(Roles.ADMINISTRATOR) ? roleOptions.get("admin") : roleOptions.get("user"))
            .build()
        ).collect(Collectors.toSet()));

        //model for creating new users
        model.addAttribute("user", UserWithRoleDTO.builder().build());

        return "users/index";
    }

    @Transactional
    @PostMapping("create")
    public String createUser(@ModelAttribute UserWithRoleDTO user) {
        System.out.println("user = " + user);

        User fullUser = User.builder()
            .userId(user.getUserId())
            .name(user.getName())
            .active(true)
            .password("Test1234")
            .build();
        Set<String> roles = new HashSet<>();
        roles.add(Roles.USER);
//            if (user.getAccessRole().equals("admin")) {
        roles.add(Roles.ADMINISTRATOR);
//            }
        fullUser.setRoles(roles);
        userService.create(fullUser);
        return "redirect:/admin/users/all";

    }
}

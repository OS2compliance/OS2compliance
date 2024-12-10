package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.dto.RoleOptionDTO;
import dk.digitalidentity.model.dto.UserWithRoleDTO;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.security.RequireAdminstrator;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
        "noaccess", new RoleOptionDTO("noaccess", "Ingen Adgang"),
        "user", new RoleOptionDTO("user", "Bruger"),
        "superuser", new RoleOptionDTO("superuser", "Superbruger"),
        "admin", new RoleOptionDTO("admin", "Administrator")
    );

    @Profile("locallogin")
    @GetMapping("all")
    public String allUsers(Model model) {
        //model for role options
        model.addAttribute("roleOptions", new ArrayList<RoleOptionDTO>(roleOptions.values()));

        //Model for list of users - do not set password for DTO!
        model.addAttribute("allUsers", userService.getAll().stream().map(user -> UserWithRoleDTO.builder()
                .uuid(user.getUuid())
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .active(user.getActive())
                .accessRole(getAccessRole(user))
                .build()
            )
            .sorted(Comparator.comparing(UserWithRoleDTO::getActive)
                .reversed()
                .thenComparing(UserWithRoleDTO::getUserId))
            .collect(Collectors.toList()));

        //model for creating new users
        model.addAttribute("user", UserWithRoleDTO.builder().build());

        //Action specified to set the posting endpoint of the fragment
        model.addAttribute("action", "create");

        return "users/index";
    }

    @Profile("locallogin")
    @GetMapping("create")
    public String createUser(Model model) {
        //model for role options
        model.addAttribute("roleOptions", new ArrayList<>(roleOptions.values()));

        //model for user being edited
        model.addAttribute("user", UserWithRoleDTO.builder().build());

        //Action specified to set the posting endpoint of the fragment
        model.addAttribute("action", "create");

        return "users/fragments/edit";
    }

    @Profile("locallogin")
    @GetMapping("edit/{id}")
    public String editUser(Model model, @PathVariable("id") final String id) {
        //model for role options
        model.addAttribute("roleOptions", new ArrayList<>(roleOptions.values()));

        //model for user being edited - do not set Password for DTO!
        User user = userService.findByUuidIncludingInactive(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));


        model.addAttribute("user", UserWithRoleDTO.builder()
            .uuid(user.getUuid())
            .userId(user.getUserId())
            .name(user.getName())
            .email(user.getEmail())
            .active(user.getActive())
            .accessRole(getAccessRole(user))
            .build());

        //Action specified to set the posting endpoint of the fragment
        model.addAttribute("action", "edit");

        return "users/fragments/edit";
    }

    @Profile("locallogin")
    @Transactional
    @PostMapping("create")
    public String createUser(@ModelAttribute UserWithRoleDTO user) {
        final User fullUser = User.builder()
            .userId(user.getUserId())
            .name(user.getName())
            .active(user.getActive())
            .email(user.getEmail())
            .password(UUID.randomUUID().toString()) // This will never work, as the password should be encoded, but setting a UUID will ensure the user cannot login before the user have reset password
            .build();
        Set<String> roles = new HashSet<>();
        if (user.getAccessRole().equals("user")) {
            roles.add(Roles.USER);
        }
        if (user.getAccessRole().equals("admin")) {
            roles.add(Roles.ADMINISTRATOR);
            roles.add(Roles.USER);
        }
        if (user.getAccessRole().equals("superuser")) {
            roles.add(Roles.SUPERUSER);
            roles.add(Roles.ADMINISTRATOR);
            roles.add(Roles.USER);
        }
        fullUser.setRoles(roles);
        userService.save(fullUser);
        return "redirect:/admin/users/all";
    }

    @Profile("locallogin")
    @Transactional
    @PostMapping("edit")
    public String editUser(@ModelAttribute UserWithRoleDTO user) {
        //find user to update
        User dbUser = userService.findByUuidIncludingInactive(user.getUuid())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        //Update values for the user
        Set<String> roles = new HashSet<>();
        if (user.getAccessRole().equals("user")) {
            roles.add(Roles.USER);
        }
        if (user.getAccessRole().equals("admin")) {
            roles.add(Roles.ADMINISTRATOR);
            roles.add(Roles.USER);
        }
        if (user.getAccessRole().equals("superuser")) {
            roles.add(Roles.SUPERUSER);
            roles.add(Roles.ADMINISTRATOR);
            roles.add(Roles.USER);
        }
        dbUser.setRoles(roles);

        String updatedName = user.getName();
        String existingName = dbUser.getName();
        if (updatedName != null && !updatedName.equals(existingName) && !updatedName.isEmpty()) {
            dbUser.setName(updatedName);
        }

        String updatedUserId = user.getUserId();
        String existingUserId = dbUser.getUserId();
        if (updatedUserId != null && !updatedUserId.equals(existingUserId) && !updatedUserId.isEmpty()) {
            dbUser.setUserId(updatedUserId);
        }

        String updatedEmail = user.getEmail();
        String existingEmail = dbUser.getEmail();
        if (updatedEmail != null && !updatedEmail.equals(existingEmail) && !updatedEmail.isEmpty()) {
            dbUser.setEmail(updatedEmail);
        }

        Boolean updatedActiveStatus = user.getActive();
        Boolean existingctiveStatus = dbUser.getActive();
        if (updatedActiveStatus != null && !updatedActiveStatus.equals(existingctiveStatus)) {
            dbUser.setActive(updatedActiveStatus);
        }

        //save user to database
        userService.save(dbUser);

        return "redirect:/admin/users/all";
    }


    private static String getAccessRole(User user) {
        if (user.getRoles().contains(Roles.SUPERUSER)) {
            return "superuser";
        } else if(user.getRoles().contains(Roles.ADMINISTRATOR)){
            return "admin";
        }else if (user.getRoles().contains(Roles.USER)) {
            return "user";
        } else {
            return "noaccess";
        }
    }
}

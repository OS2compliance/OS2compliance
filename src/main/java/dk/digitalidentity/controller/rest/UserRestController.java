package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.mapping.UserMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.UserDTO;
import dk.digitalidentity.model.dto.UserWithRoleDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.security.RequireAdministrator;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.FilterService;
import dk.digitalidentity.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("rest/users")
@RequireUser
@RequiredArgsConstructor
public class UserRestController {
    private final UserDao userDao;
    private final UserMapper userMapper;
    final private UserService userService;
    private final AssetService assetService;
    private final FilterService filterService;

    @PostMapping("all")
    public PageDTO<UserWithRoleDTO> list(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "limit", defaultValue = "50") int limit,
        @RequestParam(value = "order", required = false) String sortColumn,
        @RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
        @RequestParam Map<String, String> filters // Dynamic filters for search fields
    ) {
        Page<User> users =  userDao.findAllWithColumnSearch(
            filterService.validateSearchFilters(filters, User.class),
            null,
            filterService.buildPageable(page, limit, sortColumn, sortDirection),
            User.class
        );

        return new PageDTO<>(users.getTotalElements(), userMapper.toDTOWithRole(users.getContent()));
    }

    @GetMapping("autocomplete")
    public PageDTO<UserDTO> autocomplete(@RequestParam("search") final String search) {
        final Pageable page = PageRequest.of(0, 25, Sort.by("name").ascending());
        if (StringUtils.length(search) == 0) {
            return userMapper.toDTO(userDao.findAll(page));
        } else {
            final String replacedString = search.replace(' ', '%');
            return userMapper.toDTO(userDao.searchForUser("%" + replacedString + "%", page));
        }

    }

    @Transactional
    @DeleteMapping("delete/{userUuid}")
    @RequireAdministrator
    public ResponseEntity<?> deleteUser(@PathVariable("userUuid") final String userUuid) {
        User user = userService.findByUuidIncludingInactive(userUuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        userService.delete(user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Transactional
    @PostMapping("reset/{userUuid}")
    @RequireAdministrator
    public ResponseEntity<?> resetPasswordUser(@PathVariable("userUuid") final String userID) {
        final User user = userService.findByUuid(userID)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        userService.sendForgottenPasswordMail(user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    public record RoleSelectionDTO(Long assetId, List<Long> selectedRoleIds) {
    }

    @PostMapping("{userUuid}/role/add")
    public ResponseEntity<?> addAssetFragment(@PathVariable String userUuid, @RequestBody RoleSelectionDTO roleselection) {

        final User user = userService.findByUuid(userUuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Asset asset = assetService.findById(roleselection.assetId)
            .orElseThrow();

        asset.getRoles().stream()
            .filter(role -> role.getAsset().equals(asset))
            .forEach(role -> {
                if (role.getUsers().contains(user) && !roleselection.selectedRoleIds.contains(role.getId())) {
                    role.getUsers().remove(user);

                } else if (!role.getUsers().contains(user) && roleselection.selectedRoleIds.contains(role.getId())) {
                    role.getUsers().add(user);
                }
            });

        assetService.save(asset);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    public record Note(String note) {
    }

    @Transactional
    @PutMapping("{userUuid}/note")
    @RequireAdministrator
    public ResponseEntity<?> updateUserNote(@PathVariable("userUuid") final String userID, @RequestBody Note note) {
        User user = userService.findByUuid(userID)
            .orElseThrow();

        user.setNote(note.note);
        userService.save(user);

        return new ResponseEntity<>(HttpStatus.OK);
    }

}

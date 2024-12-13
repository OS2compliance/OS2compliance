package dk.digitalidentity.controller.rest.Assets;


import dk.digitalidentity.dao.RoleDao;
import dk.digitalidentity.mapping.AssetMapper;
import dk.digitalidentity.mapping.RoleMapper;
import dk.digitalidentity.model.dto.AssetDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.RoleDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.Role;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.AssetGrid;
import dk.digitalidentity.security.RequireSuperuser;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.RoleService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("rest/asset/roles")
@RequireUser
@RequiredArgsConstructor
public class RolesRestController {
private final RoleDao roleDao;
    private final RoleMapper mapper;
    private final RoleService roleService;
    private final AssetService assetService;
    private final UserService userService;

    @PostMapping("{assetId}")
    public List<RoleDTO> list(@PathVariable(name="assetId") final Long assetId,
                              @RequestParam(name = "order", required = false) final String order,
                              @RequestParam(name = "dir", required = false) final String dir) {

        Asset asset = assetService.findById(assetId)
            .orElseThrow();

        return asset.getRoles().stream().map(mapper::toDTO).sorted(Comparator.comparing(RoleDTO::name)).toList();
    }

    public record RoleWithUserSelection(RoleDTO roleDTO, Set<String> userIds) {}
    @RequireSuperuser
    @PostMapping("edit")
    public ResponseEntity<?> createRole(@RequestBody final RoleWithUserSelection roleWithUserSelection) {
        RoleDTO roleDTO = roleWithUserSelection.roleDTO;
        System.out.println("roleWithUserSelection = " + roleWithUserSelection.userIds);

        Role role = new Role();
        if (roleDTO.id() != null) {
            role = roleService.getRole(roleDTO.id())
                .orElseThrow();
        } else {
            role.setAsset(assetService.findById(roleDTO.assetId())
                .orElseThrow());
        }

        boolean updated = false;

        String updatedName = roleDTO.name();
        if (updatedName != null
            && !updatedName.isEmpty()
            && !updatedName.equals(role.getName())
        ) {
            role.setName(roleDTO.name());
            updated = true;
        }

        List<User> updatedUsers =  userService.findAllByUuids(roleWithUserSelection.userIds);
        if  (!updatedUsers.equals(role.getUsers())) {
            role.setUsers(new HashSet<>(updatedUsers));
            updated = true;
        }

        if (updated) {
            roleService.save(role);
        }


        return new ResponseEntity<>(HttpStatus.OK);
    }



    @RequireSuperuser
    @DeleteMapping("delete/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        if (id != null) {
            roleService.delete(id);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}

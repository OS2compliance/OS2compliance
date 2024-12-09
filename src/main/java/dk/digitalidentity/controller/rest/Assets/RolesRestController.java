package dk.digitalidentity.controller.rest.Assets;


import dk.digitalidentity.dao.RoleDao;
import dk.digitalidentity.mapping.AssetMapper;
import dk.digitalidentity.mapping.RoleMapper;
import dk.digitalidentity.model.dto.AssetDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.RoleDTO;
import dk.digitalidentity.model.entity.Role;
import dk.digitalidentity.model.entity.grid.AssetGrid;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("rest/asset/roles")
@RequireUser
@RequiredArgsConstructor
public class RolesRestController {

    private final RoleDao roleDao;
    private final RoleMapper mapper;

    @PostMapping()
    public PageDTO<RoleDTO> list(@RequestParam(name = "search", required = false) final String search,
                                 @RequestParam(name = "page", required = false, defaultValue = "0") final Integer page,
                                 @RequestParam(name = "size", required = false, defaultValue = "50") final Integer size,
                                 @RequestParam(name = "order", required = false) final String order,
                                 @RequestParam(name = "dir", required = false) final String dir) {
        Sort sort = null;
        if (StringUtils.isNotEmpty(order) && containsField(order)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, order);
        } else {
            sort = Sort.by(Sort.Direction.ASC, "name");
        }
        final Pageable sortAndPage = PageRequest.of(page, size, sort);
        Page<Role> roles = null;
        if (StringUtils.isNotEmpty(search)) {
            final List<String> searchableProperties = Arrays.asList("name");
            // search and page
            roles = roleDao.findAllCustom(searchableProperties, search, sortAndPage, Role.class);
        } else {
            // Fetch paged and sorted
            roles = roleDao.findAll(sortAndPage);
        }

        return new PageDTO<>(roles.getTotalElements(), mapper.toDTO(roles.getContent()));
    }

    private boolean containsField(final String fieldName) {
        return fieldName.equals("name");
    }
}

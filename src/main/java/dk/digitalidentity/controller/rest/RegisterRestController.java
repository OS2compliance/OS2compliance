package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.RegisterGridDao;
import dk.digitalidentity.mapping.RegisterMapper;
import dk.digitalidentity.model.dto.AssetDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.RegisterDTO;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.AssetGrid;
import dk.digitalidentity.model.entity.grid.RegisterGrid;
import dk.digitalidentity.security.RequireSuperuser;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("rest/registers")
@RequireUser
@RequiredArgsConstructor
public class RegisterRestController {
    private final RegisterGridDao registerGridDao;
    private final RegisterMapper mapper;
    private final UserService userService;

    @PostMapping("list")
    public PageDTO<RegisterDTO> list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "order", required = false) String sortColumn,
            @RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
            @RequestParam Map<String, String> filters // Dynamic filters for search fields
    ) {
        // Remove pagination/sorting parameters from the filter map
        filters.remove("page");
        filters.remove("limit");
        filters.remove("order");
        filters.remove("dir");

        //Set sorting
        Sort sort = null;
        if (StringUtils.isNotEmpty(sortColumn) && containsField(sortColumn)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(sortDirection).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, sortColumn);
        } else {
            sort = Sort.unsorted();
        }
        final Pageable sortAndPage = PageRequest.of(page, limit, sort);

        Page<RegisterGrid> registers =  registerGridDao.findAllWithColumnSearch(filters, null, sortAndPage, RegisterGrid.class);

        assert registers != null;
        return new PageDTO<>(registers.getTotalElements(), mapper.toDTO(registers.getContent()));
    }

    @PostMapping("list/{id}")
    public PageDTO<RegisterDTO> list(
        @PathVariable(name = "id") final String uuid,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "limit", defaultValue = "50") int limit,
        @RequestParam(value = "order", required = false) String sortColumn,
        @RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
        @RequestParam Map<String, String> filters // Dynamic filters for search fields
    ) {
        // Remove pagination/sorting parameters from the filter map
        filters.remove("page");
        filters.remove("limit");
        filters.remove("order");
        filters.remove("dir");

        final User user = userService.findByUuid(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!SecurityUtil.isSuperUser() && !uuid.equals(SecurityUtil.getPrincipalUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        //Set sorting
        Sort sort = null;
        if (StringUtils.isNotEmpty(sortColumn) && containsField(sortColumn)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(sortDirection).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, sortColumn);
        } else {
            sort = Sort.unsorted();
        }
        final Pageable sortAndPage = PageRequest.of(page, limit, sort);

        Page<RegisterGrid> registers = null;

        registers = registerGridDao.findAllForResponsibleUser(filters, sortAndPage, RegisterGrid.class, user);

        assert registers != null;
        return new PageDTO<>(registers.getTotalElements(), mapper.toDTO(registers.getContent()));
    }


    private boolean containsField(final String fieldName) {
        return fieldName.equals("name") || fieldName.equals("responsibleUserNames") || fieldName.equals("responsibleOUNames")
                || fieldName.equals("updatedAt") || fieldName.equals("consequenceOrder") || fieldName.equals("riskOrder") || fieldName.equals("departmentNames") || fieldName.equals("assetAssessmentOrder")
                || fieldName.equals("statusOrder") || fieldName.equals("assetCount");
    }
}

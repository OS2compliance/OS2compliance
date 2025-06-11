package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.RegisterGridDao;
import dk.digitalidentity.mapping.RegisterMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.RegisterDTO;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.RegisterGrid;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.FilterService;
import dk.digitalidentity.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

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
        Page<RegisterGrid> registers =  registerGridDao.findAllWithColumnSearch(
            validateSearchFilters(filters, RegisterGrid.class),
            null,
            buildPageable(page, limit, sortColumn, sortDirection),
            RegisterGrid.class
        );

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
        final User user = userService.findByUuid(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!SecurityUtil.isSuperUser() && !uuid.equals(SecurityUtil.getPrincipalUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Page<RegisterGrid> registers = registerGridDao.findAllForResponsibleUser(validateSearchFilters(filters, RegisterGrid.class), buildPageable(page, limit, sortColumn, sortDirection), RegisterGrid.class, user);

        assert registers != null;
        return new PageDTO<>(registers.getTotalElements(), mapper.toDTO(registers.getContent()));
    }


    private boolean containsField(final String fieldName) {
        return fieldName.equals("name") || fieldName.equals("responsibleUserNames") || fieldName.equals("responsibleOUNames")
                || fieldName.equals("updatedAt") || fieldName.equals("consequenceOrder") || fieldName.equals("riskOrder") || fieldName.equals("departmentNames") || fieldName.equals("assetAssessmentOrder")
                || fieldName.equals("statusOrder") || fieldName.equals("assetCount");
    }
}

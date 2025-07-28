package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.TaskGridDao;
import dk.digitalidentity.mapping.TaskMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.TaskDTO;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.TaskGrid;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
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
@RequestMapping("rest/tasks")
@RequireUser
@RequiredArgsConstructor
public class TaskRestController {
    private final UserService userService;
    private final TaskGridDao taskGridDao;
    private final TaskMapper mapper;

    @PostMapping("list")
    public PageDTO<TaskDTO> list(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "limit", defaultValue = "50") int limit,
        @RequestParam(value = "order", defaultValue = "nextDeadline", required = false) String sortColumn,
        @RequestParam(value = "dir", defaultValue = "asc", required = false) String sortDirection,
        @RequestParam Map<String, String> filters // Dynamic filters for search fields
    ) {
        Page<TaskGrid> tasks =  taskGridDao.findAllWithColumnSearch(
            validateSearchFilters(filters, TaskGrid.class),
            buildPageable(page, limit, sortColumn, sortDirection),
            TaskGrid.class
        );

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assert tasks != null;
        return new PageDTO<>(tasks.getTotalElements(), mapper.toDTO(tasks.getContent(), authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)), SecurityUtil.getPrincipalUuid()));
    }

    @PostMapping("list/{id}")
    public PageDTO<TaskDTO> list(
        @PathVariable(name = "id") final String userUuid,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "limit", defaultValue = "50") int limit,
        @RequestParam(value = "order", required = false) String sortColumn,
        @RequestParam(value = "dir", defaultValue = "asc") String sortDirection,
        @RequestParam Map<String, String> filters // Dynamic filters for search fields
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !SecurityUtil.getPrincipalUuid().equals(userUuid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        final User user = userService.findByUuid(userUuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Page<TaskGrid> tasks = taskGridDao.findAllForResponsibleUser(
            validateSearchFilters(filters, TaskGrid.class),
            buildPageable(page, limit, sortColumn, sortDirection),
            TaskGrid.class, user
        );

        assert tasks != null;

        return new PageDTO<>(tasks.getTotalElements(), mapper.toDTO(tasks.getContent()));
    }

}

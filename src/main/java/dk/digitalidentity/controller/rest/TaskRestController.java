package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.TaskGridDao;
import dk.digitalidentity.mapping.TaskMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.TaskDTO;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.TaskGrid;
import dk.digitalidentity.security.RequireSuperuser;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
        @RequestParam(value = "sortColumn", defaultValue = "nextDeadline", required = false) String sortColumn,
        @RequestParam(value = "sortDirection", defaultValue = "asc", required = false) String sortDirection,
        @RequestParam Map<String, String> filters // Dynamic filters for search fields
    ) {
        // Remove pagination/sorting parameters from the filter map
        filters.remove("page");
        filters.remove("limit");
        filters.remove("sortColumn");
        filters.remove("sortDirection");

        Sort sort = null;
        if (StringUtils.isNotEmpty(sortColumn)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(sortDirection).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, sortColumn);
        } else {
            sort = Sort.by(Sort.Direction.ASC, "nextDeadline");
        }
        final Pageable sortAndPage = PageRequest.of(page, limit, sort);

        Page<TaskGrid> tasks = null;

        if (!filters.isEmpty()) {
            // search and page
            final List<String> searchableProperties = Arrays.asList("name", "taskType", "responsibleUser.name", "responsibleOU.name", "nextDeadline", "localizedEnums", "tags");
            tasks = taskGridDao.findAllCustom(searchableProperties, filters.values().toString(), sortAndPage, TaskGrid.class);
        } else {
            // Fetch paged and sorted
            tasks = taskGridDao.findAll(sortAndPage);
        }


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
        @RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
        @RequestParam Map<String, String> filters // Dynamic filters for search fields
    ) {
        // Remove pagination/sorting parameters from the filter map
        filters.remove("page");
        filters.remove("limit");
        filters.remove("order");
        filters.remove("dir");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !SecurityUtil.getPrincipalUuid().equals(userUuid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        final User user = userService.findByUuid(userUuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        //Set sorting
        Sort sort = null;
        if (StringUtils.isNotEmpty(sortColumn) && containsField(sortColumn)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(sortDirection).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, sortColumn);
        } else {
            sort = Sort.by(Sort.Direction.ASC, "nextDeadline");
        }
        final Pageable sortAndPage = PageRequest.of(page, limit, sort);
        final Page<TaskGrid> tasks;

        //Get objects, filtered
        if (!filters.isEmpty()) {
            //TODO - Modify findAllCustomExtra to get all if the filter is empty, and to properly utilize filter to search columns
            // search and page
            final List<String> searchableProperties = filters.keySet().stream().toList();
            tasks = taskGridDao.findAllCustomExtra(searchableProperties, StringUtils.join(filters.values(), ", "), List.of(Pair.of("responsibleUser", user), Pair.of("completed", false)), sortAndPage, TaskGrid.class);
        } else {
            // Fetch paged and sorted
            tasks = taskGridDao.findAllByResponsibleUserAndCompletedFalse(user, sortAndPage);
        }
        assert tasks != null;
        return new PageDTO<>(tasks.getTotalElements(), mapper.toDTO(tasks.getContent()));
    }

    private boolean containsField(final String fieldName) {
        return fieldName.equals("name") || fieldName.equals("taskType") || fieldName.equals("responsibleUser.name") || fieldName.equals("responsibleOU.name")
            || fieldName.equals("nextDeadline") || fieldName.equals("taskRepetition")
            || fieldName.equals("taskResult") || fieldName.equals("tags") || fieldName.equals("completed");
    }
}

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

@Slf4j
@RestController
@RequestMapping("rest/tasks")
@RequireUser
@RequiredArgsConstructor
public class TaskRestController {
    private final UserService userService;
    private final TaskGridDao taskGridDao;
    private final TaskMapper mapper;

    @RequireSuperuser
    @PostMapping("list")
    public PageDTO<TaskDTO> list(
            @RequestParam(name = "search", required = false) final String search,
            @RequestParam(name = "page", required = false, defaultValue = "0") final Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "50") final Integer size,
            @RequestParam(name = "order", required = false) final String order,
            @RequestParam(name = "dir", required = false) final String dir) {
        Sort sort = null;
        if (StringUtils.isNotEmpty(order) && containsField(order)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, order);
        } else {
            sort = Sort.by(Sort.Direction.ASC, "nextDeadline");
        }
        final Pageable sortAndPage = PageRequest.of(page, size, sort);
        Page<TaskGrid> tasks = null;
        if (StringUtils.isNotEmpty(search)) {
            // search and page
            final List<String> searchableProperties = Arrays.asList("name", "taskType", "responsibleUser.name", "responsibleOU.name", "nextDeadline", "localizedEnums", "tags");
            tasks = taskGridDao.findAllCustom(searchableProperties, search, sortAndPage, TaskGrid.class);
        } else {
            // Fetch paged and sorted
            tasks = taskGridDao.findAll(sortAndPage);
        }
        assert tasks != null;
        return new PageDTO<>(tasks.getTotalElements(), mapper.toDTO(tasks.getContent()));
    }

    @PostMapping("list/{id}")
    public PageDTO<TaskDTO> list(
        @PathVariable(name = "id") final String userUuid,
        @RequestParam(name = "search", required = false) final String search,
        @RequestParam(name = "page", required = false, defaultValue = "0") final Integer page,
        @RequestParam(name = "size", required = false, defaultValue = "50") final Integer size,
        @RequestParam(name = "order", required = false) final String order,
        @RequestParam(name = "dir", required = false) final String dir) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER) && authentication.getPrincipal() != userUuid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Sort sort = null;
        final User user = userService.findByUuid(userUuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (StringUtils.isNotEmpty(order) && containsField(order)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, order);
        } else {
            sort = Sort.by(Sort.Direction.ASC, "nextDeadline");
        }
        final Pageable sortAndPage = PageRequest.of(page, size, sort);
        final Page<TaskGrid> tasks;
        if (StringUtils.isNotEmpty(search)) {
            // search and page
            final List<String> searchableProperties = Arrays.asList("name", "responsibleOU.name", "nextDeadline", "localizedEnums", "completed", "tags");
            tasks = taskGridDao.findAllCustomExtra(searchableProperties, search, List.of(Pair.of("responsibleUser", user), Pair.of("completed", false)), sortAndPage, TaskGrid.class);
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

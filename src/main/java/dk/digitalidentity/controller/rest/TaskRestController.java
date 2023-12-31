package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.dao.grid.TaskGridDao;
import dk.digitalidentity.mapping.TaskMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.TaskDTO;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.TaskGrid;
import dk.digitalidentity.security.RequireUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("rest/tasks")
@RequireUser
public class TaskRestController {
    @Autowired
    private final UserDao userDao;
    private final TaskGridDao taskGridDao;
    private final TaskMapper mapper;

    public TaskRestController(final UserDao userDao, final TaskGridDao taskGridDao, final TaskMapper mapper) {
        this.userDao = userDao;
        this.taskGridDao = taskGridDao;
        this.mapper = mapper;
    }

    @PostMapping("list")
    public PageDTO<TaskDTO> list(
            @RequestParam(name = "search", required = false) final String search,
            @RequestParam(name = "page", required = false) final Integer page,
            @RequestParam(name = "size", required = false) final Integer size,
            @RequestParam(name = "order", required = false) final String order,
            @RequestParam(name = "dir", required = false) final String dir) {
        Sort sort = null;
        if (StringUtils.isNotEmpty(order) && containsField(order)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, order);
        }
        final Pageable sortAndPage = sort != null ?  PageRequest.of(page, size, sort) : PageRequest.of(page, size);
        Page<TaskGrid> tasks = null;
        if (StringUtils.isNotEmpty(search)) {
            // search and page
            final List<String> searchableProperties = Arrays.asList("name", "taskType", "responsibleUser.name" , "responsibleOU.name", "nextDeadline", "localizedEnums");
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
        @PathVariable(name = "id", required = true) final String uuid,
        @RequestParam(name = "search", required = false) final String search,
        @RequestParam(name = "page", required = false) final Integer page,
        @RequestParam(name = "size", required = false) final Integer size,
        @RequestParam(name = "order", required = false) final String order,
        @RequestParam(name = "dir", required = false) final String dir) {
        Sort sort = null;
        final User user = userDao.findByUuidAndActiveIsTrue(uuid);
        if (StringUtils.isNotEmpty(order) && containsField(order)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, order);
        }
        final Pageable sortAndPage = sort != null ?  PageRequest.of(page, size, sort) : PageRequest.of(page, size);
        Page<TaskGrid> tasks = null;
        if (StringUtils.isNotEmpty(search)) {
            // search and page
            final List<String> searchableProperties = Arrays.asList("name", "responsibleOU.name", "nextDeadline", "localizedEnums", "completed");
            tasks = taskGridDao.findAllCustomForResponsibleUser(searchableProperties, search, sortAndPage, TaskGrid.class, user);
        } else {
            // Fetch paged and sorted
            tasks = taskGridDao.findAllByResponsibleUser(user, sortAndPage);
        }
        assert tasks != null;
        return new PageDTO<>(tasks.getTotalElements(), mapper.toDTO(tasks.getContent()));
    }

    private boolean containsField(final String fieldName) {
        return fieldName.equals("name") || fieldName.equals("taskType") || fieldName.equals("responsibleUser") || fieldName.equals("responsibleOU")
                || fieldName.equals("nextDeadline") || fieldName.equals("taskRepetition")
                || fieldName.equals("status");
    }
}

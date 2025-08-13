package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.TaskGridDao;
import dk.digitalidentity.mapping.TaskMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.TaskDTO;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.TaskGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireTask;
import dk.digitalidentity.service.ExcelExportService;
import dk.digitalidentity.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponse;
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

import java.io.IOException;
import java.util.List;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

@Slf4j
@RestController
@RequestMapping("rest/tasks")
@RequireTask
@RequiredArgsConstructor
public class TaskRestController {
    private final UserService userService;
    private final TaskGridDao taskGridDao;
    private final TaskMapper mapper;
	private final ExcelExportService excelExportService;

	@RequireReadOwnerOnly
    @PostMapping("list")
	public PageDTO<TaskDTO> list(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "order", defaultValue = "nextDeadline", required = false) String sortColumn,
			@RequestParam(value = "dir", defaultValue = "asc", required = false) String sortDirection,
			@RequestParam(value = "export", defaultValue = "false") boolean export,
			@RequestParam(value = "fileName", defaultValue = "export.xlsx") String fileName,
			@RequestParam Map<String, String> filters, // Dynamic filters for search fields
			HttpServletResponse response
	) throws IOException {
		final String userUuid = SecurityUtil.getLoggedInUserUuid();
		if (userUuid == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		final User user = userService.findByUuid(userUuid)
				.orElseThrow();

		// For export mode, get ALL records (no pagination)
		if (export) {
			Page<TaskGrid> allTasks;
			if (SecurityUtil.isOperationAllowed(Roles.READ_ALL)) {
				allTasks = taskGridDao.findAllWithColumnSearch(
						validateSearchFilters(filters, TaskGrid.class),
						buildPageable(page, Integer.MAX_VALUE, sortColumn, sortDirection),
						TaskGrid.class
				);
			}
			else {
				// Logged in user can see only own
				allTasks = taskGridDao.findAllWithAssignedUser(
						validateSearchFilters(filters, TaskGrid.class),
						user,
						buildPageable(page, limit, sortColumn, sortDirection),
						TaskGrid.class
				);
			}

			List<TaskDTO> allData = mapper.toDTO(allTasks.getContent());
			excelExportService.exportToExcel(allData, fileName, response);
			return null;
		}

		Page<TaskGrid> tasks;
		if (SecurityUtil.isOperationAllowed(Roles.READ_ALL)) {
			// Logged in user can see all
			tasks = taskGridDao.findAllWithColumnSearch(
					validateSearchFilters(filters, TaskGrid.class),
					buildPageable(page, limit, sortColumn, sortDirection),
					TaskGrid.class
			);
		}
		else {
			// Logged in user can see only own
			tasks = taskGridDao.findAllWithAssignedUser(
					validateSearchFilters(filters, TaskGrid.class),
					user,
					buildPageable(page, limit, sortColumn, sortDirection),
					TaskGrid.class
			);
		}

        assert tasks != null;
        return new PageDTO<>(tasks.getTotalElements(), mapper.toDTO(tasks.getContent()));
    }

	@RequireReadOwnerOnly
    @PostMapping("list/{id}")
    public PageDTO<TaskDTO> list(
        @PathVariable(name = "id") final String userUuid,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "limit", defaultValue = "50") int limit,
        @RequestParam(value = "order", required = false) String sortColumn,
        @RequestParam(value = "dir", defaultValue = "asc") String sortDirection,
        @RequestParam Map<String, String> filters // Dynamic filters for search fields
    ) {

        final User user = userService.findByUuid(userUuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Page<TaskGrid> tasks = taskGridDao.findAllWithAssignedUser(
				validateSearchFilters(filters, TaskGrid.class),
				user,
				buildPageable(page, limit, sortColumn, sortDirection),
				TaskGrid.class
		);

        assert tasks != null;

        return new PageDTO<>(tasks.getTotalElements(), mapper.toDTO(tasks.getContent()));
    }

}

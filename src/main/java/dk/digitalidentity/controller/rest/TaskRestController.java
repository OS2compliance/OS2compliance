package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.TaskGridDao;
import dk.digitalidentity.mapping.TaskMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.TaskDTO;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.TaskGrid;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireTask;
import dk.digitalidentity.service.ExcelExportService;
import dk.digitalidentity.service.SecurityUserService;
import dk.digitalidentity.service.tag.TagService;
import dk.digitalidentity.service.TaskService;
import dk.digitalidentity.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
	private final SecurityUserService securityUserService;
	private final TaskService taskService;
	private final TagService tagService;

	@RequireReadOwnerOnly
    @PostMapping("list")
	public PageDTO<TaskDTO> list(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "order", defaultValue = "nextDeadline", required = false) String sortColumn,
			@RequestParam(value = "dir", defaultValue = "asc", required = false) String sortDirection,
			@RequestParam Map<String, String> filters // Dynamic filters for search fields
	) {
		User user = securityUserService.getCurrentUserOrThrow();

		Map<Long, Tag> tagsById = tagService.findAll().stream()
				.collect(Collectors.toMap(Tag::getId, t -> t, (a, b) -> b));


		Page<TaskGrid> tasks = taskService.getTasks(sortColumn, sortDirection, filters, page, limit, user);

        assert tasks != null;
        return new PageDTO<>(tasks.getTotalElements(), mapper.toDTO(tasks.getContent(), tagsById));
    }

	@RequireReadOwnerOnly
	@PostMapping("export")
	public void export(
			@RequestParam(value = "order", required = false) String sortColumn,
			@RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
			@RequestParam(value = "fileName", defaultValue = "export.xlsx") String fileName,
			@RequestParam Map<String, String> filters,
			HttpServletResponse response
	) throws IOException {
		User user = securityUserService.getCurrentUserOrThrow();

		// Fetch all records (no pagination)
		Page<TaskGrid> tasks = taskService.getTasks(sortColumn, sortDirection, filters, 0, Integer.MAX_VALUE, user);

		Map<Long, Tag> tagsById = tagService.findAll().stream()
				.collect(Collectors.toMap(Tag::getId, t -> t, (a, b) -> b));

		assert tasks != null;
		List<TaskDTO> allData = mapper.toDTO(tasks.getContent(), tagsById);
		excelExportService.exportToExcel(allData, TaskDTO.class, fileName, response);
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
		log.info("Listing tasks for user {} with principal id {}", user.getUuid(), SecurityUtil.getPrincipalUuid());

        Page<TaskGrid> tasks = taskGridDao.findAllWithAssignedUser(
				validateSearchFilters(filters, TaskGrid.class),
				user,
				buildPageable(page, limit, sortColumn, sortDirection),
				TaskGrid.class
		);

		Map<Long, Tag> tagsById = tagService.findAll().stream()
				.collect(Collectors.toMap(Tag::getId, t -> t, (a, b) -> b));

        assert tasks != null;

        return new PageDTO<>(tasks.getTotalElements(), mapper.toDTO(tasks.getContent(), tagsById));
    }

}

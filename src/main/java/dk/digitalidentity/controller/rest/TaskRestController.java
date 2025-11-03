package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.TaskGridDao;
import dk.digitalidentity.mapping.TaskMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.TaskCreateRequestDTO;
import dk.digitalidentity.model.dto.TaskDTO;
import dk.digitalidentity.model.entity.CustomThreat;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLink;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import dk.digitalidentity.model.entity.grid.TaskGrid;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.security.annotations.crud.RequireCreateOwnerOnly;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireTask;
import dk.digitalidentity.service.ExcelExportService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.SecurityUserService;
import dk.digitalidentity.service.ThreatAssessmentService;
import dk.digitalidentity.service.tag.TagService;
import dk.digitalidentity.service.TaskService;
import dk.digitalidentity.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;
import static dk.digitalidentity.util.LinkHelper.linkify;

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
	private final ThreatAssessmentService threatAssessmentService;
	private final RelationService  relationService;

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

		Page<TaskGrid> tasks = taskService.getTasks(sortColumn, sortDirection, filters, page, limit, user);

		Set<Long> taskIds = tasks.getContent().stream().map(TaskGrid::getId).collect(Collectors.toSet());
		Map<Long, Tag> tagsById = taskService.findTagsByEntityIds(taskIds).stream()
				.collect(Collectors.toMap(Tag::getId, t -> t, (a, b) -> b));

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

	@RequireCreateOwnerOnly
	@PostMapping("create")
	public ResponseEntity<?> createTask(@Valid @RequestBody final TaskCreateRequestDTO request) {
		log.info("Received task creation request: {}", request);

		// Validate and process links
		List<TaskLink> links = new ArrayList<>();
		for (TaskLink link : request.getTask().getLinks()) {
			links.add(new TaskLink(null, linkify(link.getUrl()), request.getTask()));
		}
		request.getTask().setLinks(links);

		final Task savedTask = taskService.saveTask(request.getTask());
		relationService.setRelationsAbsolute(savedTask, request.getRelations());

		if (request.getTaskRiskId() != null) {
			final ThreatAssessment threatAssessment = threatAssessmentService.findById(request.getTaskRiskId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relateret risikovurdering ikke fundet"));

			if (threatAssessment.getThreatAssessmentType().equals(ThreatAssessmentType.ASSET)) {
				final List<Relatable> relatedAssets = relationService.findAllRelatedTo(threatAssessment).stream()
						.filter(t -> t.getRelationType().equals(RelationType.ASSET)).toList();
				taskService.addRelations(savedTask, relatedAssets);
			}
			else if (threatAssessment.getThreatAssessmentType().equals(ThreatAssessmentType.REGISTER)) {
				final List<Relatable> relatedRegisters = relationService.findAllRelatedTo(threatAssessment).stream()
						.filter(t -> t.getRelationType().equals(RelationType.REGISTER)).toList();
				taskService.addRelations(savedTask, relatedRegisters);
			}

			if (request.getRiskCustomId() != null && request.getRiskCustomId() != 0) {
				final CustomThreat threat = threatAssessment.getCustomThreats().stream()
						.filter(t -> t.getId().equals(request.getRiskCustomId()))
						.findAny()
						.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

				ThreatAssessmentResponse response = threatAssessment.getThreatAssessmentResponses().stream()
						.filter(r -> r.getCustomThreat() != null && r.getCustomThreat().getId().equals(request.getRiskCustomId()))
						.findAny().orElse(null);

				if (response == null) {
					response = threatAssessmentService.createResponse(threatAssessment, null, threat);
					threatAssessmentService.save(threatAssessment);
				}

				relationService.addRelation(savedTask, response);

			} else if (request.getRiskCatalogIdentifier() != null && !request.getRiskCatalogIdentifier().isEmpty()) {
				final ThreatCatalogThreat threat = threatAssessment.getThreatCatalogs().stream()
						.flatMap(catalog -> catalog.getThreats().stream())
						.filter(t -> t.getIdentifier().equals(request.getRiskCatalogIdentifier()))
						.findAny()
						.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

				ThreatAssessmentResponse response = threatAssessment.getThreatAssessmentResponses().stream()
						.filter(r -> r.getThreatCatalogThreat() != null &&
								r.getThreatCatalogThreat().getIdentifier().equals(request.getRiskCatalogIdentifier()))
						.findAny().orElse(null);

				if (response == null) {
					response = threatAssessmentService.createResponse(threatAssessment, threat, null);
					threatAssessmentService.save(threatAssessment);
				}

				relationService.addRelation(savedTask, response);
			}

			relationService.addRelation(savedTask, threatAssessment);

			return new ResponseEntity<>(HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

}

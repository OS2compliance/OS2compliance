package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.RegisterGridDao;
import dk.digitalidentity.mapping.RegisterMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.RegisterDTO;
import dk.digitalidentity.model.dto.excel.EntityListItemDTO;
import dk.digitalidentity.model.dto.excel.EntityListRequest;
import dk.digitalidentity.model.dto.excel.ExcelExportRequest;
import dk.digitalidentity.model.dto.excel.ExportMetadataDTO;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.RegisterGrid;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireRegister;
import dk.digitalidentity.service.ExcelExportHelperService;
import dk.digitalidentity.service.RegisterService;
import dk.digitalidentity.service.SecurityUserService;
import dk.digitalidentity.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

@Slf4j
@RestController
@RequestMapping("rest/registers")
@RequireRegister
@RequiredArgsConstructor
public class RegisterRestController {
	private final RegisterGridDao registerGridDao;
    private final RegisterMapper mapper;
    private final UserService userService;
	private final RegisterService registerService;
	private final SecurityUserService securityUserService;
	private final ExcelExportHelperService excelExportHelperService;

	@RequireReadOwnerOnly
    @PostMapping("list")
    public PageDTO<RegisterDTO> list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "order", required = false) String sortColumn,
            @RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
            @RequestParam Map<String, String> filters // Dynamic filters for search fields
    ) {
		User user = securityUserService.getCurrentUserOrThrow();

		Page<RegisterGrid> registers = registerService.getRegisters(sortColumn, sortDirection, filters, page, limit, user);

		Set<Long> entityIds = registers.getContent().stream().map(RegisterGrid::getId).collect(Collectors.toSet());
		Map<Long, Tag> tagsById = registerService.findTagsByEntityIds(entityIds).stream()
				.collect(Collectors.toMap(Tag::getId, t -> t, (a, b) -> b));

		assert registers != null;
        return new PageDTO<>(registers.getTotalElements(), mapper.toDTO(registers.getContent(),tagsById));
    }

	@RequireReadOwnerOnly
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

		Page<RegisterGrid> registers = registerGridDao.findAllWithAssignedUser(
				validateSearchFilters(filters, RegisterGrid.class),
				user,
				buildPageable(page, limit, sortColumn, sortDirection),
				RegisterGrid.class
		);

		Set<Long> entityIds = registers.getContent().stream().map(RegisterGrid::getId).collect(Collectors.toSet());
		Map<Long, Tag> tagsById = registerService.findTagsByEntityIds(entityIds).stream()
				.collect(Collectors.toMap(Tag::getId, t -> t, (a, b) -> b));

        assert registers != null;
        return new PageDTO<>(registers.getTotalElements(), mapper.toDTO(registers.getContent(), tagsById));
    }

	@GetMapping("export-metadata")
	@RequireReadOwnerOnly
	public ExportMetadataDTO getExportMetadata() {
		return excelExportHelperService.getMetadata(RegisterDTO.class);
	}

	@PostMapping("export-entities")
	@RequireReadOwnerOnly
	public List<EntityListItemDTO> getEntitiesForExport(@RequestBody EntityListRequest request) {
		User user = securityUserService.getCurrentUserOrThrow();
		Page<RegisterGrid> registers = registerService.getRegisters(
				null,
				"ASC",
				request.getFilters(),
				0,
				Integer.MAX_VALUE,
				user
		);

		return excelExportHelperService.toEntityListItems(
				registers.getContent(),
				RegisterGrid::getId,
				RegisterGrid::getName
		);
	}

	@PostMapping("export-custom")
	@RequireReadOwnerOnly
	public void exportCustom(
			@RequestBody ExcelExportRequest request,
			HttpServletResponse response
	) throws IOException {
		User user = securityUserService.getCurrentUserOrThrow();
		List<RegisterGrid> registerGrids = registerService.findByIds(request.getSelectedIds(), user);

		if (registerGrids.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		excelExportHelperService.exportEntitiesWithTags(
				registerGrids,
				RegisterDTO.class,
				mapper::toDTO,
				registerService::findTagsByEntityIds,
				RegisterGrid::getId,
				request,
				response
		);
	}
}

package dk.digitalidentity.controller.rest;

import dk.digitalidentity.mapping.IncidentMapper;
import dk.digitalidentity.model.dto.IncidentDTO;
import dk.digitalidentity.model.dto.IncidentFieldDTO;
import dk.digitalidentity.model.dto.IncidentQuery;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.excel.EntityListItemDTO;
import dk.digitalidentity.model.dto.excel.EntityListRequest;
import dk.digitalidentity.model.dto.excel.ExcelExportRequest;
import dk.digitalidentity.model.dto.excel.ExportMetadataDTO;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.IncidentField;
import dk.digitalidentity.security.annotations.crud.RequireDeleteAll;
import dk.digitalidentity.security.annotations.crud.RequireReadAll;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.crud.RequireUpdateAll;
import dk.digitalidentity.security.annotations.sections.RequireConfiguration;
import dk.digitalidentity.service.ExcelExportHelperService;
import dk.digitalidentity.service.IncidentService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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

import static dk.digitalidentity.service.FilterService.buildPageable;

@Slf4j
@RestController
@RequestMapping("rest/incidents")
@RequireConfiguration
@RequiredArgsConstructor
public class IncidentRestController {
    private static final String DEFAULT_SORT_COLUMN = "createdAt";

    private final IncidentService incidentService;
    private final IncidentMapper incidentMapper;
	private final ExcelExportHelperService excelExportHelperService;

    @RequireReadAll
    @GetMapping("questions")
    public List<IncidentFieldDTO> list() {
        return incidentMapper.toFieldDTOs(incidentService.getAllFields());
    }

	@RequireDeleteAll
    @DeleteMapping("questions/{id}")
    @Transactional
    public ResponseEntity<?> deleteQuestion(@PathVariable final Long id) {
        final IncidentField fieldToDelete = incidentService.findField(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        incidentService.deleteField(fieldToDelete);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

	@RequireUpdateAll
    @Transactional
    @PostMapping("questions/{id}/up")
    public ResponseEntity<?> questionReorderUp(@PathVariable("id") final Long id) {
        final IncidentField fieldToReorder = incidentService.findField(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        incidentService.reorderField(fieldToReorder, false);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

	@RequireUpdateAll
    @Transactional
    @PostMapping("questions/{id}/down")
    public ResponseEntity<?> questionReorderDown(@PathVariable("id") final Long id) {
        final IncidentField fieldToReorder = incidentService.findField(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        incidentService.reorderField(fieldToReorder, true);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

	@RequireDeleteAll
    @DeleteMapping("{id}")
    @Transactional
    public ResponseEntity<?> deleteIncident(@PathVariable final Long id) {
        final Incident incidentToDelete = incidentService.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        incidentService.delete(incidentToDelete);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequireReadAll
    @PostMapping("list")
    public PageDTO<IncidentDTO> list(
        @RequestParam(name = "page", required = false, defaultValue = "0") final int page,
        @RequestParam(name = "limit", required = false, defaultValue = "50") final int limit,
        @RequestParam(name = "order", required = false) final String order,
        @RequestParam(name = "dir", required = false) final String dir,
        @RequestParam final Map<String, String> filters
	) {
        final Page<Incident> incidents = incidentService.findIncidents(
            IncidentQuery.of(filters), pageable(page, limit, order, dir));
        return new PageDTO<>(incidents.getTotalElements(), incidentMapper.toDTOs(incidents.getContent()));
    }

	@RequireReadAll
    @GetMapping("columns")
    public List<IncidentFieldDTO> visibleColumns() {
        return incidentMapper.toFieldDTOs(incidentService.getAllFields().stream()
            .filter(field -> StringUtils.isNotEmpty(field.getIndexColumnName()))
            .toList());
    }

    /**
     * The date fields the incident log can filter its from/to range on, on top of the built-in
     * created and updated timestamps.
     */
    @RequireReadAll
    @GetMapping("datefields")
    public List<IncidentFieldDTO> dateFields() {
        return incidentMapper.toFieldDTOs(incidentService.getDateFields());
    }

	@GetMapping("export-metadata")
	@RequireReadOwnerOnly
	public ExportMetadataDTO getExportMetadata() {
		return excelExportHelperService.getMetadata(IncidentDTO.class);
	}

	@PostMapping("export-entities")
	@RequireReadOwnerOnly
	public List<EntityListItemDTO> getEntitiesForExport(@RequestBody EntityListRequest request) {
		// The filters map is the same one the grid sends, so the export sees exactly the list the user sees
		final Page<Incident> incidents = incidentService.findIncidents(
				IncidentQuery.of(request.getFilters()),
				pageable(0, Integer.MAX_VALUE, request.getSortColumn(), request.getSortDirection()));

		return excelExportHelperService.toEntityListItems(
				incidents.getContent(),
				Incident::getId,
				Incident::getName
		);
	}

	@PostMapping("export-custom")
	@RequireReadOwnerOnly
	public void exportCustom(
			@RequestBody ExcelExportRequest request,
			HttpServletResponse response
	) throws IOException {

		if (request.getSelectedIds() == null || request.getSelectedIds().isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		List<Long> ids = request.getSelectedIds().stream()
				.map(Long::parseLong)
				.toList();
		List<Incident> incidents = incidentService.findByIds(ids);

		if (incidents.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		List<IncidentDTO> dtos = incidentMapper.toDTOs(incidents);

		excelExportHelperService.exportEntities(
				IncidentDTO.class,
				dtos,
				request,
				response
		);
	}

	@GetMapping(value = "fields/export-metadata")
	@RequireReadOwnerOnly
	public ExportMetadataDTO getExportMetadataField() {
		return excelExportHelperService.getMetadata(IncidentFieldDTO.class);
	}

	@PostMapping("fields/export-entities")
	@RequireReadOwnerOnly
	public List<EntityListItemDTO> getFieldEntitiesForExport(@RequestBody EntityListRequest request) {
		List<IncidentFieldDTO> fields = incidentMapper.toFieldDTOs(incidentService.getAllFields());

		return excelExportHelperService.toEntityListItems(
				fields,
				IncidentFieldDTO::getId,
				IncidentFieldDTO::getQuestion
		);
	}

	@PostMapping("fields/export-custom")
	@RequireReadOwnerOnly
	public void exportCustomField(
			@RequestBody ExcelExportRequest request,
			HttpServletResponse response
	) throws IOException {
		List<Long> ids = request.getSelectedIds().stream()
				.map(Long::parseLong)
				.toList();

		List<IncidentField> fields = incidentService.findFieldsByIds(ids);

		if (fields.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		excelExportHelperService.exportEntities(
				fields,
				IncidentFieldDTO.class,
				incidentMapper::toFieldDTOs,
				request,
				response
		);
	}

	/**
	 * Sorting is only offered on the built-in columns; custom field columns live in a separate table
	 * and cannot be reached from a Pageable. An unknown column falls back to newest first rather than
	 * failing the request.
	 */
	private static Pageable pageable(final int page, final int limit, final String order, final String dir) {
		if (StringUtils.isNotEmpty(order) && IncidentQuery.BUILT_IN_COLUMNS.contains(order)) {
			return buildPageable(page, limit, order, dir);
		}
		return buildPageable(page, limit, DEFAULT_SORT_COLUMN, Sort.Direction.DESC.name());
	}

}

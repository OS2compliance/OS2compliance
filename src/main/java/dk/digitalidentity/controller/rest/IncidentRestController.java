package dk.digitalidentity.controller.rest;

import dk.digitalidentity.controller.mvc.IncidentController;
import dk.digitalidentity.mapping.IncidentMapper;
import dk.digitalidentity.model.ExcelColumn;
import dk.digitalidentity.model.ExcludeFromExport;
import dk.digitalidentity.model.dto.IncidentDTO;
import dk.digitalidentity.model.dto.IncidentFieldDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.excel.EntityListItemDTO;
import dk.digitalidentity.model.dto.excel.EntityListRequest;
import dk.digitalidentity.model.dto.excel.ExcelExportRequest;
import dk.digitalidentity.model.dto.excel.ExportMetadataDTO;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.IncidentField;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.DocumentGrid;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("rest/incidents")
@RequireConfiguration
@RequiredArgsConstructor
public class IncidentRestController {
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
        @RequestParam(name = "search", required = false) final String search,
        @RequestParam(name = "page", required = false, defaultValue = "0") final Integer page,
        @RequestParam(name = "size", required = false, defaultValue = "50") final Integer size,
        @RequestParam(name = "order", required = false) final String order,
        @RequestParam(name = "dir", required = false) final String dir,
        @RequestParam(name = "fromDate", required = false) @DateTimeFormat(pattern = "dd/MM-yyyy") final LocalDate fromDateParam,
        @RequestParam(name = "toDate", required = false) @DateTimeFormat(pattern = "dd/MM-yyyy") final LocalDate toDateParam
	) {
        Sort sort;
        if (StringUtils.isNotEmpty(order)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, order);
        } else {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }
        final Pageable sortAndPage = PageRequest.of(page, size, sort);
		Page<Incident> incidents = incidentService.getIncidents(search, fromDateParam, toDateParam, sortAndPage);

		assert incidents != null;
		return new PageDTO<>(incidents.getTotalElements(), incidentMapper.toDTOs(incidents.getContent()));
    }

	@RequireReadAll
    @GetMapping("columns")
    public List<String> visibleColumns() {
        return incidentService.getAllFields().stream()
            .map(IncidentField::getIndexColumnName)
            .filter(StringUtils::isNotEmpty)
            .toList();
    }

	@GetMapping("export-metadata")
	@RequireReadOwnerOnly
	public ExportMetadataDTO getExportMetadata() {
		return excelExportHelperService.getMetadata(IncidentDTO.class);
	}

	@PostMapping("export-entities")
	@RequireReadOwnerOnly
	public List<EntityListItemDTO> getEntitiesForExport(@RequestBody EntityListRequest request) {
		// Extract date filters from request filters map
		LocalDate fromDate = extractDateFromFilters(request.getFilters(), "fromDate");
		LocalDate toDate = extractDateFromFilters(request.getFilters(), "toDate");
		String search = request.getFilters().getOrDefault("search", null);

		// Build sort
		Sort sort;
		if (StringUtils.isNotEmpty(request.getSortColumn())) {
			Sort.Direction direction = "DESC".equalsIgnoreCase(request.getSortDirection())
					? Sort.Direction.DESC
					: Sort.Direction.ASC;
			sort = Sort.by(direction, request.getSortColumn());
		} else {
			sort = Sort.by(Sort.Direction.DESC, "createdAt");
		}

		Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, sort);
		Page<Incident> incidents = incidentService.getIncidents(search, fromDate, toDate, pageable);

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

	/**
	 * Extract date from filters map with format dd/MM-yyyy
	 */
	private LocalDate extractDateFromFilters(Map<String, String> filters, String key) {
		if (filters == null || !filters.containsKey(key)) {
			return null;
		}

		String dateStr = filters.get(key);
		if (dateStr == null || dateStr.isBlank()) {
			return null;
		}

		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM-yyyy");
			return LocalDate.parse(dateStr, formatter);
		} catch (Exception e) {
			return null;
		}
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

}

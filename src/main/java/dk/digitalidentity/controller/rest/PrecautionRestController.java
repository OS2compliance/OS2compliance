package dk.digitalidentity.controller.rest;

import dk.digitalidentity.mapping.ThreatMapper;
import dk.digitalidentity.model.ExcelColumn;
import dk.digitalidentity.model.ExcludeFromExport;
import dk.digitalidentity.model.dto.excel.ExcelExportRequest;
import dk.digitalidentity.model.dto.excel.ExportMetadataDTO;
import dk.digitalidentity.model.entity.Precaution;
import dk.digitalidentity.security.annotations.crud.RequireDeleteAll;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireConfiguration;
import dk.digitalidentity.service.ExcelExportHelperService;
import dk.digitalidentity.service.PrecautionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequireConfiguration
@RequiredArgsConstructor
@RequestMapping(value = "rest/precautions", consumes = "application/json", produces = "application/json")
public class PrecautionRestController {
    private final PrecautionService precautionService;
    private final ThreatMapper threatMapper;
	private final ExcelExportHelperService excelExportHelperService;

	@RequireDeleteAll
    @Transactional
    @DeleteMapping(value = "{precautionIdentifier}")
    public ResponseEntity<?> delete(@PathVariable("precautionIdentifier") final Long precautionIdentifier) {
        final Precaution catalog = precautionService.get(precautionIdentifier)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        precautionService.delete(catalog);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

	record PrecautionListExportDTO(
			@ExcludeFromExport
			Long id,
			@ExcelColumn(headerName = "Foranstaltning", order = 1)
			String name,
			@ExcelColumn(headerName = "Beskrivelse", order = 2)
			String description
	) {}

	@GetMapping(value = "export-metadata", consumes = "*/*")
	@RequireReadOwnerOnly
	public ExportMetadataDTO getExportMetadata() {
		return excelExportHelperService.getMetadata(PrecautionListExportDTO.class);
	}

	@PostMapping("export-custom")
	@RequireReadOwnerOnly
	public void exportCustom(
			@RequestBody ExcelExportRequest request,
			HttpServletResponse response
	) throws IOException {
		List<Long> ids = request.getSelectedIds().stream()
				.map(Long::parseLong)
				.toList();

		List<Precaution> precautions = precautionService.findByIds(ids);

		if (precautions.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		List<PrecautionListExportDTO> dtos = precautions.stream()
				.map(p -> new PrecautionListExportDTO(
						p.getId(),
						p.getName(),
						p.getDescription()
				))
				.toList();

		excelExportHelperService.exportEntities(
				PrecautionListExportDTO.class,
				dtos,
				request,
				response
		);
	}

}



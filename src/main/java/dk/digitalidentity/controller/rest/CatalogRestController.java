package dk.digitalidentity.controller.rest;

import dk.digitalidentity.mapping.ThreatMapper;
import dk.digitalidentity.model.ExcelColumn;
import dk.digitalidentity.model.ExcludeFromExport;
import dk.digitalidentity.model.dto.ThreatCatalogThreatDTO;
import dk.digitalidentity.model.dto.excel.ExcelExportRequest;
import dk.digitalidentity.model.dto.excel.ExportMetadataDTO;
import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.security.annotations.crud.RequireDeleteAll;
import dk.digitalidentity.security.annotations.crud.RequireReadAll;
import dk.digitalidentity.security.annotations.crud.RequireUpdateAll;
import dk.digitalidentity.security.annotations.sections.RequireConfiguration;
import dk.digitalidentity.service.CatalogService;
import dk.digitalidentity.service.ExcelExportHelperService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequireConfiguration
@RequiredArgsConstructor
@RequestMapping(value = "rest/catalogs")
public class CatalogRestController {
    private final CatalogService catalogService;
    private final ThreatMapper threatMapper;
	private final ExcelExportHelperService excelExportHelperService;

	@RequireDeleteAll
    @Transactional
    @DeleteMapping(value = "{catalogIdentifier}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> delete(@PathVariable("catalogIdentifier") final String catalogIdentifier) {
        final ThreatCatalog catalog = catalogService.get(catalogIdentifier)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        catalogService.delete(catalog);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

	@RequireReadAll
    @Transactional
    @GetMapping(value = "{catalogIdentifier}", produces = "application/json")
    public List<ThreatCatalogThreatDTO> list(@PathVariable("catalogIdentifier") final String catalogIdentifier) {
        final ThreatCatalog catalog = catalogService.get(catalogIdentifier)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return catalog.getThreats().stream()
            .map(t -> {
                final ThreatCatalogThreatDTO dto = threatMapper.toDTO(t);
                dto.setInUse(catalogService.threatInUse(t));
                return dto;
            })
            .collect(Collectors.toList());
    }

	@RequireDeleteAll
    @Transactional
    @DeleteMapping(value = "{catalogIdentifier}/{identifier}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> delete(@PathVariable("catalogIdentifier") final String catalogIdentifier,
                                    @PathVariable("identifier") final String identifier) {
        final ThreatCatalog catalog = catalogService.get(catalogIdentifier)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final ThreatCatalogThreat threat = catalogService.getThreat(identifier)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        catalogService.deleteThreat(threat);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

	@RequireUpdateAll
    @Transactional
    @PostMapping(value = "{catalogIdentifier}/{identifier}/up", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> reorderUp(@PathVariable("catalogIdentifier") final String catalogIdentifier,
                                    @PathVariable("identifier") final String identifier) {
        reorder(catalogIdentifier, identifier, false);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

	@RequireUpdateAll
    @Transactional
    @PostMapping(value = "{catalogIdentifier}/{identifier}/down", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> reorderDown(@PathVariable("catalogIdentifier") final String catalogIdentifier,
                                                    @PathVariable("identifier") final String identifier) {
        reorder(catalogIdentifier, identifier, true);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private void reorder(final String catalogIdentifier, final String identifier, final boolean backwards) {
        final ThreatCatalog catalog = catalogService.get(catalogIdentifier)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final ThreatCatalogThreat threat = catalogService.getThreat(identifier)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Threats an only be reordered between threats of same type
        final List<ThreatCatalogThreat> threatsWithSameType = catalog.getThreats().stream()
            .filter(t -> StringUtils.equalsIgnoreCase(threat.getThreatType(), t.getThreatType()))
            .sorted(sortCatalogThreatsComparator(backwards))
            .toList();
        if (!threatsWithSameType.isEmpty()) {
            // Lowest is ordered on top
            ThreatCatalogThreat last = null;
            for (final ThreatCatalogThreat currentThreat : threatsWithSameType) {
                if (last != null && currentThreat.getIdentifier().equals(identifier)) {
                    final Long newKey = last.getSortKey();
                    last.setSortKey(currentThreat.getSortKey());
                    currentThreat.setSortKey(newKey);
                    break;
                }
                last = currentThreat;
            }
        }
    }

    private static Comparator<ThreatCatalogThreat> sortCatalogThreatsComparator(final boolean backwards) {
        final Comparator<ThreatCatalogThreat> comparator = Comparator.comparing(ThreatCatalogThreat::getSortKey);
        return backwards ? comparator.reversed() : comparator;
    }

	record CatalogExportDTO(
			@ExcludeFromExport
			String identifier,
			@ExcelColumn(headerName = "Katalog", order = 1)
			String name,
			@ExcelColumn(headerName = "Trusler", order = 2)
			int threatCount,
			@ExcelColumn(headerName = "Synlighed", order = 3)
			String visibility
	) {}

	@GetMapping("export-metadata")
	@RequireReadAll
	public ExportMetadataDTO getExportMetadata() {
		return excelExportHelperService.getMetadata(CatalogExportDTO.class);
	}

	@PostMapping("export-custom")
	@RequireReadAll
	public void exportCustom(
			@RequestBody ExcelExportRequest request,
			HttpServletResponse response
	) throws IOException {
		List<ThreatCatalog> catalogs = request.getSelectedIds().stream()
				.map(catalogService::get)
				.filter(opt -> opt.isPresent())
				.map(opt -> opt.get())
				.toList();

		if (catalogs.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		List<CatalogExportDTO> dtos = catalogs.stream()
				.map(c -> new CatalogExportDTO(
						c.getIdentifier(),
						c.getName(),
						c.getThreats() != null ? c.getThreats().size() : 0,
						c.isHidden() ? "Skjult" : "Synlig"
				))
				.toList();

		excelExportHelperService.exportEntities(
				CatalogExportDTO.class,
				dtos,
				request,
				response
		);
	}

}



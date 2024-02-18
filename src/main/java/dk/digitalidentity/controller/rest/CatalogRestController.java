package dk.digitalidentity.controller.rest;

import dk.digitalidentity.mapping.ThreatMapper;
import dk.digitalidentity.model.dto.ThreatCatalogThreatDTO;
import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.CatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Slf4j
@RestController
@RequireUser
@RequiredArgsConstructor
@RequestMapping(value = "rest/catalogs", consumes = "application/json", produces = "application/json")
public class CatalogRestController {
    private final CatalogService catalogService;
    private final ThreatMapper threatMapper;

    @GetMapping(value = "{catalogIdentifier}")
    public List<ThreatCatalogThreatDTO> list(@PathVariable("catalogIdentifier") final String catalogIdentifier) {
        final ThreatCatalog catalog = catalogService.get(catalogIdentifier)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return threatMapper.toDTO(catalog.getThreats());
    }

    @PostMapping("{catalogIdentifier}/{identifier}/up")
    public List<ThreatCatalogThreatDTO> reorderUp(@PathVariable("catalogIdentifier") final String catalogIdentifier,
                                                  @PathVariable("identifier") final String identifier) {
        return reorder(catalogIdentifier, identifier, false);
    }

    @PostMapping("{catalogIdentifier}/{identifier}/down")
    public List<ThreatCatalogThreatDTO> reorderDown(@PathVariable("catalogIdentifier") final String catalogIdentifier,
                                                    @PathVariable("identifier") final String identifier) {
        return reorder(catalogIdentifier, identifier, true);
    }

    private List<ThreatCatalogThreatDTO> reorder(final String catalogIdentifier, final String identifier, final boolean backwards) {
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
        return threatMapper.toDTO(catalog.getThreats());
    }

    private static Comparator<ThreatCatalogThreat> sortCatalogThreatsComparator(final boolean backwards) {
        final Comparator<ThreatCatalogThreat> comparator = Comparator.comparing(ThreatCatalogThreat::getSortKey);
        return backwards ? comparator.reversed() : comparator;
    }

}



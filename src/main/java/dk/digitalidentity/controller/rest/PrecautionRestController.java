package dk.digitalidentity.controller.rest;

import dk.digitalidentity.mapping.ThreatMapper;
import dk.digitalidentity.model.dto.ThreatCatalogThreatDTO;
import dk.digitalidentity.model.entity.Precaution;
import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.security.RequireAdminstrator;
import dk.digitalidentity.service.PrecautionService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequireAdminstrator
@RequiredArgsConstructor
@RequestMapping(value = "rest/precautions", consumes = "application/json", produces = "application/json")
public class PrecautionRestController {
    private final PrecautionService precautionService;
    private final ThreatMapper threatMapper;

    @Transactional
    @DeleteMapping(value = "{precautionIdentifier}")
    public ResponseEntity<?> delete(@PathVariable("precautionIdentifier") final Long precautionIdentifier) {
        final Precaution catalog = precautionService.get(precautionIdentifier)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        precautionService.delete(catalog);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

//    @Transactional
//    @GetMapping(value = "{catalogIdentifier}")
//    public List<ThreatCatalogThreatDTO> list(@PathVariable("catalogIdentifier") final String catalogIdentifier) {
//        final ThreatCatalog catalog = precautionService.get(catalogIdentifier)
//            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
//        return catalog.getThreats().stream()
//            .map(t -> {
//                final ThreatCatalogThreatDTO dto = threatMapper.toDTO(t);
//                dto.setInUse(precautionService.threatInUse(t));
//                return dto;
//            })
//            .collect(Collectors.toList());
//    }
//
//    @Transactional
//    @DeleteMapping("{catalogIdentifier}/{identifier}")
//    public ResponseEntity<?> delete(@PathVariable("catalogIdentifier") final String catalogIdentifier,
//                                    @PathVariable("identifier") final String identifier) {
//        final ThreatCatalog catalog = precautionService.get(catalogIdentifier)
//            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
//        final ThreatCatalogThreat threat = precautionService.getThreat(identifier)
//            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
//        precautionService.deleteThreat(threat);
//        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//    }
//
//    @Transactional
//    @PostMapping("{catalogIdentifier}/{identifier}/up")
//    public ResponseEntity<?> reorderUp(@PathVariable("catalogIdentifier") final String catalogIdentifier,
//                                    @PathVariable("identifier") final String identifier) {
//        reorder(catalogIdentifier, identifier, false);
//        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//    }
//
//    @Transactional
//    @PostMapping("{catalogIdentifier}/{identifier}/down")
//    public ResponseEntity<?> reorderDown(@PathVariable("catalogIdentifier") final String catalogIdentifier,
//                                                    @PathVariable("identifier") final String identifier) {
//        reorder(catalogIdentifier, identifier, true);
//        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//    }
//
//    private void reorder(final String catalogIdentifier, final String identifier, final boolean backwards) {
//        final ThreatCatalog catalog = precautionService.get(catalogIdentifier)
//            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
//        final ThreatCatalogThreat threat = precautionService.getThreat(identifier)
//            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
//        // Threats an only be reordered between threats of same type
//        final List<ThreatCatalogThreat> threatsWithSameType = catalog.getThreats().stream()
//            .filter(t -> StringUtils.equalsIgnoreCase(threat.getThreatType(), t.getThreatType()))
//            .sorted(sortCatalogThreatsComparator(backwards))
//            .toList();
//        if (!threatsWithSameType.isEmpty()) {
//            // Lowest is ordered on top
//            ThreatCatalogThreat last = null;
//            for (final ThreatCatalogThreat currentThreat : threatsWithSameType) {
//                if (last != null && currentThreat.getIdentifier().equals(identifier)) {
//                    final Long newKey = last.getSortKey();
//                    last.setSortKey(currentThreat.getSortKey());
//                    currentThreat.setSortKey(newKey);
//                    break;
//                }
//                last = currentThreat;
//            }
//        }
//    }
//
//    private static Comparator<ThreatCatalogThreat> sortCatalogThreatsComparator(final boolean backwards) {
//        final Comparator<ThreatCatalogThreat> comparator = Comparator.comparing(ThreatCatalogThreat::getSortKey);
//        return backwards ? comparator.reversed() : comparator;
//    }

}



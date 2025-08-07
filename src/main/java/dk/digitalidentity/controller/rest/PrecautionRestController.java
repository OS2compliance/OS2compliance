package dk.digitalidentity.controller.rest;

import dk.digitalidentity.mapping.ThreatMapper;
import dk.digitalidentity.model.entity.Precaution;
import dk.digitalidentity.security.annotations.RequireAdministrator;
import dk.digitalidentity.service.PrecautionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequireAdministrator
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

}



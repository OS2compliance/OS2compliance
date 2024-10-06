package dk.digitalidentity.controller.rest;

import dk.digitalidentity.mapping.IncidentMapper;
import dk.digitalidentity.model.dto.IncidentFieldDTO;
import dk.digitalidentity.model.entity.IncidentField;
import dk.digitalidentity.security.RequireAdminstrator;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.IncidentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("rest/incidents")
@RequireUser
@RequiredArgsConstructor
public class IncidentRestController {
    private final IncidentService incidentService;
    private final IncidentMapper incidentMapper;

    @RequireAdminstrator
    @GetMapping("questions")
    public List<IncidentFieldDTO> list() {
        return incidentMapper.toDtos(incidentService.getAllFields());
    }

    @DeleteMapping("questions/{id}")
    @Transactional
    public ResponseEntity<?> deleteQuestion(@PathVariable final Long id) {
        final IncidentField fieldToDelete = incidentService.findField(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        incidentService.deleteField(fieldToDelete);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Transactional
    @PostMapping("questions/{id}/up")
    public ResponseEntity<?> questionReorderUp(@PathVariable("id") final Long id) {
        final IncidentField fieldToReorder = incidentService.findField(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        incidentService.reorderField(fieldToReorder, false);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Transactional
    @PostMapping("questions/{id}/down")
    public ResponseEntity<?> questionReorderDown(@PathVariable("id") final Long id) {
        final IncidentField fieldToReorder = incidentService.findField(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        incidentService.reorderField(fieldToReorder, true);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}

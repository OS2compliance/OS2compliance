package dk.digitalidentity.controller.rest;

import dk.digitalidentity.mapping.IncidentMapper;
import dk.digitalidentity.model.dto.IncidentDTO;
import dk.digitalidentity.model.dto.IncidentFieldDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.IncidentField;
import dk.digitalidentity.security.RequireAdministrator;
import dk.digitalidentity.security.RequireSuperuserOrAdministrator;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.IncidentService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("rest/incidents")
@RequireSuperuserOrAdministrator
@RequiredArgsConstructor
public class IncidentRestController {
    private final IncidentService incidentService;
    private final IncidentMapper incidentMapper;

    @RequireAdministrator
    @GetMapping("questions")
    public List<IncidentFieldDTO> list() {
        return incidentMapper.toFieldDTOs(incidentService.getAllFields());
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

    @DeleteMapping("{id}")
    @Transactional
    public ResponseEntity<?> deleteIncident(@PathVariable final Long id) {
        final Incident incidentToDelete = incidentService.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        incidentService.delete(incidentToDelete);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequireUser
    @PostMapping("list")
    public PageDTO<IncidentDTO> list(
        @RequestParam(name = "search", required = false) final String search,
        @RequestParam(name = "page", required = false, defaultValue = "0") final Integer page,
        @RequestParam(name = "size", required = false, defaultValue = "50") final Integer size,
        @RequestParam(name = "order", required = false) final String order,
        @RequestParam(name = "dir", required = false) final String dir,
        @RequestParam(name = "fromDate", required = false) @DateTimeFormat(pattern = "dd/MM-yyyy") final LocalDate fromDateParam,
        @RequestParam(name = "toDate", required = false) @DateTimeFormat(pattern = "dd/MM-yyyy") final LocalDate toDateParam) {
        Sort sort = null;
        if (StringUtils.isNotEmpty(order)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, order);
        } else {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }
        final Pageable sortAndPage = PageRequest.of(page, size, sort);
        final LocalDateTime fromDate = fromDateParam != null ? fromDateParam.atStartOfDay() : LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
        final LocalDateTime toDate = toDateParam != null ? toDateParam.plusDays(1).atStartOfDay() : LocalDateTime.of(3000, 1, 1, 0, 0);
        final Page<Incident> incidents = StringUtils.isNotEmpty(search)
            ? incidentService.search(search, fromDate, toDate, sortAndPage)
            : incidentService.listIncidents(fromDate, toDate, sortAndPage);

        assert incidents != null;
        return new PageDTO<>(incidents.getTotalElements(), incidentMapper.toDTOs(incidents.getContent()));
    }

    @RequireUser
    @GetMapping("columns")
    public List<String> visibleColumns() {
        return incidentService.getAllFields().stream()
            .map(IncidentField::getIndexColumnName)
            .filter(StringUtils::isNotEmpty)
            .toList();
    }

}

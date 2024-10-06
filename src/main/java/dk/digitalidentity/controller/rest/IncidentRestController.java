package dk.digitalidentity.controller.rest;

import dk.digitalidentity.mapping.IncidentMapper;
import dk.digitalidentity.model.dto.IncidentFieldDTO;
import dk.digitalidentity.security.RequireAdminstrator;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}

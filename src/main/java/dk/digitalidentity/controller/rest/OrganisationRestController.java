package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.OrganisationUnitDao;
import dk.digitalidentity.mapping.OrganisationUnitMapper;
import dk.digitalidentity.model.dto.OrganisationUnitDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.Position;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("rest/ous")
@RequireUser
@RequiredArgsConstructor
public class OrganisationRestController {
    private final OrganisationUnitDao organisationUnitDao;
    private final OrganisationUnitMapper mapper;
    private final UserService userService;

    @GetMapping("autocomplete")
    public PageDTO<OrganisationUnitDTO> autocomplete(@RequestParam("search") final String search) {
        final Pageable page = PageRequest.of(0, 25, Sort.by("name").ascending());
        if (StringUtils.length(search) == 0) {
            return mapper.toDTO(organisationUnitDao.findAllByActiveTrue(page));
        } else {
            return mapper.toDTO(organisationUnitDao.searchForOU("%" + search + "%", page));
        }
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<OrganisationUnitDTO> getOrgUuidByUser(@PathVariable final String id) {
        final User user = userService.findByUuid(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final Position position = user.getPositions().stream()
            .filter(p -> p.getOuUuid() != null)
            .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(mapper.toDTO(organisationUnitDao.findByUuid(position.getOuUuid())));
    }
}

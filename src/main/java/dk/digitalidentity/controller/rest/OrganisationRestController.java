package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.OrganisationUnitDao;
import dk.digitalidentity.mapping.OrganisationUnitMapper;
import dk.digitalidentity.model.dto.OrganisationUnitDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.security.RequireUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("rest/ous")
@RequireUser
public class OrganisationRestController {
    private final OrganisationUnitDao organisationUnitDao;
    private final OrganisationUnitMapper mapper;

    public OrganisationRestController(final OrganisationUnitDao organisationUnitDao, final OrganisationUnitMapper mapper) {
        this.organisationUnitDao = organisationUnitDao;
        this.mapper = mapper;
    }


    @GetMapping("autocomplete")
    public PageDTO<OrganisationUnitDTO> autocomplete(@RequestParam("search") final String search) {
        final Pageable page = PageRequest.of(0, 25, Sort.by("name").ascending());
        if (StringUtils.length(search) == 0) {
            return mapper.toDTO(organisationUnitDao.findAll(page));
        } else {
            return mapper.toDTO(organisationUnitDao.searchForOU("%" + search + "%", page));
        }
    }
}

package dk.digitalidentity.service;


import dk.digitalidentity.dao.OrganisationUnitDao;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class OrganisationService {
    private final OrganisationUnitDao organisationUnitDao;

    public OrganisationService(final OrganisationUnitDao organisationUnitDao) {
        this.organisationUnitDao = organisationUnitDao;
    }

    public Optional<OrganisationUnit> get(final String uuid) {
        if (uuid == null) {
            return Optional.empty();
        }
        return organisationUnitDao.findById(uuid);
    }

    public Page<OrganisationUnit> getPaged(final int pageSize, final int page) {
        return organisationUnitDao.findAll(Pageable.ofSize(pageSize).withPage(page));
    }

    public List<OrganisationUnit> findAllByUuids(final Set<String> uuids) {
        return organisationUnitDao.findAllByUuidInAndActiveTrue(uuids);
    }
}

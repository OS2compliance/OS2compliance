package dk.digitalidentity.service;


import dk.digitalidentity.dao.OrganisationUnitDao;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Position;
import dk.digitalidentity.model.entity.User;
import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OrganisationService {
    private final OrganisationUnitDao organisationUnitDao;
    private final EntityManager entityManager;

    public OrganisationService(final OrganisationUnitDao organisationUnitDao, final EntityManager entityManager) {
        this.organisationUnitDao = organisationUnitDao;
        this.entityManager = entityManager;
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

    /**
     * Persists a new organisation unit, failing if the primary key is already taken.
     * Unlike {@link #save(OrganisationUnit)} this never degrades to an update of an existing row.
     */
    @Transactional
    public OrganisationUnit create(final OrganisationUnit organisationUnit) {
        entityManager.persist(organisationUnit);
        entityManager.flush();
        return organisationUnit;
    }

    @Transactional
    public OrganisationUnit save(final OrganisationUnit organisationUnit) {
        return organisationUnitDao.save(organisationUnit);
    }

	public Optional<OrganisationUnit> findByUuid(final String uuid) {
		if (StringUtils.isEmpty(uuid)) {
			return Optional.empty();
		}
		return Optional.ofNullable(organisationUnitDao.findByUuid(uuid));
	}

	/**
	 * Afdeling is derived from the user's position, forvaltning from that OU's parent.
	 */
	public Optional<OrganisationUnit> findAfdelingForUser(final User user) {
		if (user == null) {
			return Optional.empty();
		}
		final Set<String> ouUuids = user.getPositions().stream()
			.map(Position::getOuUuid)
			.filter(StringUtils::isNotEmpty)
			.collect(Collectors.toSet());
		if (ouUuids.size() != 1) {
			return Optional.empty();
		}
		return findByUuid(ouUuids.iterator().next());
	}

	public Optional<OrganisationUnit> findForvaltningForUser(final User user) {
		return findAfdelingForUser(user)
			.map(OrganisationUnit::getParentUuid)
			.flatMap(this::findByUuid);
	}
}

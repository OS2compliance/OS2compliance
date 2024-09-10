package dk.digitalidentity.integration.os2sync;

import dk.digitalidentity.dao.OrganisationUnitDao;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.integration.os2sync.api.HierarchyOU;
import dk.digitalidentity.integration.os2sync.api.HierarchyPosition;
import dk.digitalidentity.integration.os2sync.api.HierarchyResult;
import dk.digitalidentity.integration.os2sync.api.HierarchyUser;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Position;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.service.NotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Slf4j
@Component
public class OS2SyncService {
    @Autowired
    private OrganisationUnitDao organisationUnitDao;
    @Autowired
    private UserDao userDao;
    @Autowired
    private NotifyService notifyService;

    @Transactional
    public void persistHierarchy(final HierarchyResult result) {
        persistOUs(result.getOus());
        persistUsers(result.getUsers());
    }

    private void persistUsers(final List<HierarchyUser> users) {
        log.info("Persisting {} users", users.size());
        final Set<String> existingUuids = userDao.findAllActiveUuids();
        final Set<String> newUuids = users.stream().map(this::persist)
                .filter(Objects::nonNull)
                .map(User::getUuid)
                .collect(Collectors.toSet());
        existingUuids.removeAll(newUuids);
        final int deactivatedUsers = userDao.deactivateUsers(existingUuids);
        log.info("Deactivated {} users", deactivatedUsers);
        notifyService.notifyAboutInactiveUsers(existingUuids);
    }

    private void persistOUs(final List<HierarchyOU> ous) {
        log.info("Persisting {} ous", ous.size());
        final Set<String> existingUuids = organisationUnitDao.findAllActiveUuids();
        final Set<String> newUuids = ous.stream().map(this::persist)
                .filter(Objects::nonNull)
                .map(OrganisationUnit::getUuid)
                .collect(Collectors.toSet());
        existingUuids.removeAll(newUuids);
        final int deactivatedOUs = organisationUnitDao.deactivateOUs(existingUuids);
        log.info("Deactivated {} ous", deactivatedOUs);
    }

    private User persist(final HierarchyUser hierarchyUser) {
        if (hierarchyUser.getUuid() == null) {
            log.warn("Missing UUID on User " + hierarchyUser);
            return null;
        }
        final User user = userDao.findById(hierarchyUser.getUuid())
                .orElseGet(() -> createUser(hierarchyUser));
        user.setUserId(hierarchyUser.getUserId());
        user.setUuid(hierarchyUser.getUuid());
        user.setName(hierarchyUser.getName());
        user.setEmail(hierarchyUser.getEmail());
        user.getPositions().clear();
        user.getPositions().addAll(hierarchyUser.getPositions().stream()
            .map(p -> toPosition(user, p))
            .collect(Collectors.toSet()));
        user.setActive(true);

        return userDao.save(user);
    }

    private OrganisationUnit persist(final HierarchyOU hierarchyOU) {
        if (hierarchyOU.getUuid() == null) {
            log.warn("Missing UUID on OU " + hierarchyOU);
            return null;
        }
        final OrganisationUnit ou = organisationUnitDao.findById(hierarchyOU.getUuid())
                .orElseGet(() -> createOrganisationUnit(hierarchyOU));
        ou.setActive(true);
        ou.setParentUuid(hierarchyOU.getParentUUID());
        ou.setName(hierarchyOU.getName());
        return ou;
    }

    private User createUser(final HierarchyUser hierarchyUser) {
        final User user = new User();
        user.setUserId(hierarchyUser.getUserId());
        user.setUuid(hierarchyUser.getUuid());
        user.setName(hierarchyUser.getName());
        user.setActive(true);
        user.setPositions(hierarchyUser.getPositions().stream()
                .map(p -> toPosition(user, p))
                .collect(Collectors.toSet()));
        return userDao.save(user);
    }

    private OrganisationUnit createOrganisationUnit(final HierarchyOU hierarchyOU) {
        final OrganisationUnit ou = new OrganisationUnit();
        ou.setUuid(hierarchyOU.getUuid());
        ou.setName(hierarchyOU.getName());
        ou.setParentUuid(hierarchyOU.getParentUUID());
        ou.setActive(true);
        return organisationUnitDao.save(ou);
    }

    private static Position toPosition(final User user, final HierarchyPosition hierarchyPosition) {
        final Position p = new Position();
        p.setName(hierarchyPosition.getName());
        p.setOuUuid(p.getOuUuid());
        p.setOuUuid(hierarchyPosition.getOuUuid());
        p.setUser(user);
        return p;
    }

}

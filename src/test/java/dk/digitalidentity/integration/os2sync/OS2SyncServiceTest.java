package dk.digitalidentity.integration.os2sync;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.OrganisationUnitDao;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.integration.os2sync.api.HierarchyResult;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static dk.digitalidentity.integration.os2sync.TestDataGenerator.generateOU;
import static dk.digitalidentity.integration.os2sync.TestDataGenerator.generatePosition;
import static dk.digitalidentity.integration.os2sync.TestDataGenerator.generateUser;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OS2SyncService}
 */
@Transactional
public class OS2SyncServiceTest extends BaseIntegrationTest {
    @Autowired
    private OS2SyncService syncService;
    @Autowired
    private OrganisationUnitDao organisationUnitDao;
    @Autowired
    private UserDao userDao;

    @Test
    public void canPersist() {
        // When
        syncService.persistHierarchy(generatedDefaultResult());

        // Then
        final var allUsers = userDao.findAll();
        assertThat(allUsers)
                .hasSize(2)
                .extracting(User::getUuid)
                .containsExactlyInAnyOrder("u1", "u2");
        final var allOUs = organisationUnitDao.findAll();
        assertThat(allOUs)
                .hasSize(3)
                .extracting(OrganisationUnit::getUuid)
                .containsExactlyInAnyOrder("1", "2", "3");
        assertThat(allOUs)
                .extracting(OrganisationUnit::getParentUuid)
                .containsExactlyInAnyOrder(null, "1", "1");
    }

    @Test
    public void deactivateRemoved() {
        // Given
        final var deactivateUser = new User();
        deactivateUser.setUuid("removeme");
        deactivateUser.setActive(true);
        userDao.save(deactivateUser);

        final var deactivateOU = new OrganisationUnit();
        deactivateOU.setUuid("removemetoo");
        deactivateOU.setActive(true);
        organisationUnitDao.save(deactivateOU);

        // When
        syncService.persistHierarchy(generatedDefaultResult());

        // When
        final var activeUserUuids = userDao.findAllActiveUuids();
        assertThat(activeUserUuids).containsExactlyInAnyOrder("u1", "u2");
        final var activeOuUuids = organisationUnitDao.findAllActiveUuids();
        assertThat(activeOuUuids).containsExactlyInAnyOrder("1", "2", "3");
    }

    static HierarchyResult generatedDefaultResult() {
        final var result = new HierarchyResult();
        result.getOus().add(generateOU("1", "parent1", null));
        result.getOus().add(generateOU("2", "child1", "1"));
        result.getOus().add(generateOU("3", "child2", "1"));
        result.getUsers().add(generateUser("u1", "user1", "user1",
                generatePosition("Pedel", "2")));
        result.getUsers().add(generateUser("u2", "user2", "user2",
                generatePosition("Konsulent", "2"),
                generatePosition("Dyrepasser", "3")));
        return result;
    }

}

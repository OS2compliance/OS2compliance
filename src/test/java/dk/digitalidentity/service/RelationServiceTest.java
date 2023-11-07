package dk.digitalidentity.service;


import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.DocumentDao;
import dk.digitalidentity.dao.OrganisationUnitDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.dao.TaskDao;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.DocumentStatus;
import dk.digitalidentity.model.entity.enums.TaskType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RelationService}
 */
@Transactional
public class RelationServiceTest extends BaseIntegrationTest {
    @Autowired
    private DocumentDao documentDao;
    @Autowired
    private TaskDao taskDao;
    @Autowired
    private RelationDao relationDao;
    @Autowired
    private RelationService relationService;
    @Autowired
    private UserDao userDao;
    @Autowired
    private OrganisationUnitDao ouDao;

    @Test
    public void canFindNamesOfRelated() {
        // Given
        final var user1 = createTestUser(UUID.randomUUID().toString(), "abo", "Amalie");
        final var user2 = createTestUser(UUID.randomUUID().toString(), "kbp", "Kaspar");
        final var ou1 = createTestOU(UUID.randomUUID().toString(), "Enhed 1");
        final var doc1 = createTestDocument("Fancy doc name", user1);
        final var doc2 = createTestDocument("Some other doc name", user2);
        final var task1 = createTestTask("Fancy task name", user1, ou1);
        final var task2 = createTestTask("Some other task name", user2, ou1);
        relateEntities(doc1, task1);
        relateEntities(doc1, task2);
        relateEntities(doc1, doc2);

        // When
        final List<Relatable> related = relationService.findAllRelatedTo(doc1);
        // Then
        assertThat(related).hasSize(3)
                .extracting(Relatable::getName)
                .containsExactlyInAnyOrder("Some other doc name", "Fancy task name", "Some other task name");
    }

    private void relateEntities(final Relatable ra, final Relatable rb) {
        relationDao.save(Relation.builder()
                        .relationAType(ra.getRelationType())
                        .relationAId(ra.getId())
                        .relationBType(rb.getRelationType())
                        .relationBId(rb.getId())
                .build());
    }

    private Document createTestDocument(final String name, final User user) {
        final var doc = new Document();
        doc.setStatus(DocumentStatus.READY);
        doc.setName(name);
        doc.setResponsibleUser(user);
        return documentDao.save(doc);
    }

    private Task createTestTask(final String name, final User user, final OrganisationUnit ou) {
        final var task = new Task();
        task.setName(name);
        task.setTaskType(TaskType.TASK);
        task.setDescription("Something something ... CAKE");
        task.setResponsibleUser(user);
        task.setResponsibleOu(ou);
        task.setNextDeadline(LocalDate.now().plusMonths(1));
        return taskDao.save(task);
    }

    private User createTestUser(final String uuid, final String initials, final String name) {
        return userDao.save(User.builder()
                .uuid(uuid)
                .userId(initials)
                .name(name)
                .build());
    }

    private OrganisationUnit createTestOU(final String uuid, final String name) {
        return ouDao.save(OrganisationUnit.builder()
                .uuid(uuid)
                .name(name)
                .active(true)
                .build());
    }

}

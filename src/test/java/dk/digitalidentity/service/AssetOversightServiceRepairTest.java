package dk.digitalidentity.service;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.Constants;
import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.dao.ChoiceValueDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.dao.TaskDao;
import dk.digitalidentity.dao.TaskLogDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLog;
import dk.digitalidentity.model.entity.enums.AssetStatus;
import dk.digitalidentity.model.entity.enums.ContainsAITechnologyEnum;
import dk.digitalidentity.model.entity.enums.Criticality;
import dk.digitalidentity.model.entity.enums.DataProcessingAgreementStatus;
import dk.digitalidentity.model.entity.enums.TaskType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DB-level integration test for {@link AssetOversightService#repairMisbookedDbsOversightLogs} proving
 * that a misbooked oversight log is MOVED to the correct DBS task rather than DELETED by the
 * {@code orphanRemoval = true} mapping on {@code Task.logs}.
 */
@Transactional
public class AssetOversightServiceRepairTest extends BaseIntegrationTest {

    @Autowired private AssetOversightService assetOversightService;
    @Autowired private AssetDao assetDao;
    @Autowired private TaskDao taskDao;
    @Autowired private TaskLogDao taskLogDao;
    @Autowired private RelationDao relationDao;
    @Autowired private ChoiceValueDao choiceValueDao;
    @PersistenceContext private EntityManager em;

    @Test
    public void movesMisbookedLogToDbsTaskWithoutDeletingIt() {
        // The DBS supervision model is seeded by DataBootstrap; reuse it (identifier is unique).
        final ChoiceValue dbsModel = choiceValueDao.findByIdentifier(Constants.DBS_SUPERVISION_MODEL_IDENTIFIER_PREFIX)
            .orElseThrow(() -> new IllegalStateException("DBS supervision model not seeded"));

        final ChoiceValue assetType = choiceValueDao.findByIdentifier(Constants.CHOICE_LIST_ASSET_IT_SYSTEM_TYPE_ID)
            .orElseThrow(() -> new IllegalStateException("IT system asset type not seeded"));

        Asset asset = new Asset();
        asset.setName("Foo");
        asset.setSupervisoryModel(dbsModel);
        asset.setAssetType(assetType);
        asset.setAssetStatus(AssetStatus.NOT_STARTED);
        asset.setAiStatus(ContainsAITechnologyEnum.UNDECIDED);
        asset.setCriticality(Criticality.CRITICAL);
        asset.setDataProcessingAgreementStatus(DataProcessingAgreementStatus.NOT_RELEVANT);
        asset = assetDao.save(asset);
        final Long assetId = asset.getId();

        final Task oldCheck = taskDao.save(oversightTask(TaskType.CHECK,
            "Udfør tilsyn med leverandøren af Foo", LocalDate.of(2026, 3, 1), assetId));
        final Task dbsTask = taskDao.save(oversightTask(TaskType.TASK,
            "Supplier - Foo - DBS tilsyn", LocalDate.of(2026, 3, 22), assetId));
        final Long oldCheckId = oldCheck.getId();
        final Long dbsTaskId = dbsTask.getId();

        relate(oldCheck, asset);
        relate(dbsTask, asset);

        // A completed tilsyn misbooked onto the old CHECK task. createdAt is @CreationTimestamp so
        // both tasks are stamped "now"; using today's date for completed keeps the causality guard
        // (completed >= task.createdAt) satisfied.
        final TaskLog misbooked = new TaskLog();
        misbooked.setName("Tilsyn udført");
        misbooked.setComment("Status: Grøn");
        misbooked.setResponsibleUserUserId("kbp");
        misbooked.setDocumentationLink("http://localhost/assets/" + assetId);
        misbooked.setCompleted(LocalDate.now());
        misbooked.setDeadline(LocalDate.of(2026, 3, 1));
        misbooked.setTask(oldCheck);
        final Long logId = taskLogDao.save(misbooked).getId();

        // Flush + clear so the repair runs against entities loaded fresh from the DB (as seedV44 does
        // in production), rather than the just-persisted instances whose collections are cached.
        em.flush();
        em.clear();

        final int moved = assetOversightService.repairMisbookedDbsOversightLogs(
            assetDao.findById(assetId).orElseThrow());
        assertThat(moved).isEqualTo(1);

        em.flush();
        em.clear(); // drop in-memory state and re-read from the DB

        final TaskLog reloaded = taskLogDao.findById(logId).orElse(null);
        assertThat(reloaded)
            .as("the log must be MOVED, not deleted by orphanRemoval")
            .isNotNull();
        assertThat(reloaded.getTask().getId()).isEqualTo(dbsTaskId);
        assertThat(taskLogDao.findByTaskIdIn(List.of(oldCheckId))).isEmpty();
        assertThat(taskLogDao.findByTaskIdIn(List.of(dbsTaskId)))
            .extracting(TaskLog::getId)
            .containsExactly(logId);
    }

    private Task oversightTask(final TaskType type, final String name, final LocalDate deadline, final Long assetId) {
        final Task task = new Task();
        task.setTaskType(type);
        task.setName(name);
        task.setNextDeadline(deadline);
        task.getProperties().add(Property.builder()
            .entity(task)
            .key(Constants.ASSOCIATED_INSPECTION_PROPERTY)
            .value("" + assetId)
            .build());
        return task;
    }

    private void relate(final Relatable ra, final Relatable rb) {
        relationDao.save(Relation.builder()
            .relationAType(ra.getRelationType())
            .relationAId(ra.getId())
            .relationBType(rb.getRelationType())
            .relationBId(rb.getId())
            .build());
    }
}

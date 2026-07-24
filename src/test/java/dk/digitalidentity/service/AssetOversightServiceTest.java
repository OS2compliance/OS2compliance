package dk.digitalidentity.service;

import dk.digitalidentity.Constants;
import dk.digitalidentity.dao.AssetOversightDao;
import dk.digitalidentity.dao.ChoiceValueDao;
import dk.digitalidentity.dao.TaskLogDao;
import dk.digitalidentity.dao.grid.DBSOversightGridDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetOversight;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLog;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.samlmodule.config.SamlModuleConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the oversight-task selection and the retroactive misbooking repair in
 * {@link AssetOversightService}. Guards against the bug where a completed tilsyn was booked onto an
 * older tilsyn task on the same asset while the real "DBS tilsyn" task stayed overdue.
 */
@ExtendWith(MockitoExtension.class)
class AssetOversightServiceTest {

    @Mock private SamlModuleConfiguration samlConfiguration;
    @Mock private AssetOversightDao assetOversightDao;
    @Mock private TaskService taskService;
    @Mock private RelationService relationService;
    @Mock private UserService userService;
    @Mock private ChoiceValueDao choiceValueDao;
    @Mock private DBSOversightGridDao dbsOversightGridDao;
    @Mock private TaskLogDao taskLogDao;

    @InjectMocks private AssetOversightService assetOversightService;

    private static final long ASSET_ID = 100L;

    private ChoiceValue dbsModel() {
        final ChoiceValue model = new ChoiceValue();
        model.setIdentifier(Constants.DBS_SUPERVISION_MODEL_IDENTIFIER_PREFIX);
        return model;
    }

    private ChoiceValue otherModel() {
        final ChoiceValue model = new ChoiceValue();
        model.setIdentifier("supervision-model-standard-123456");
        return model;
    }

    private Asset asset(final ChoiceValue supervisoryModel) {
        final Asset asset = new Asset();
        asset.setId(ASSET_ID);
        asset.setSupervisoryModel(supervisoryModel);
        return asset;
    }

    private Task oversightTask(final long id, final TaskType type, final String name, final LocalDate deadline) {
        final Task task = new Task();
        task.setId(id);
        task.setTaskType(type);
        task.setName(name);
        task.setNextDeadline(deadline);
        task.getProperties().add(Property.builder()
            .entity(task)
            .key(Constants.ASSOCIATED_INSPECTION_PROPERTY)
            .value("" + ASSET_ID)
            .build());
        return task;
    }

    @Test
    void booksDbsOversightOntoDbsTaskWhenMultipleTasksExist() {
        final Task oldCheck = oversightTask(1L, TaskType.CHECK,
            "Udfør tilsyn med leverandøren af Foo", LocalDate.of(2026, 3, 1));
        final Task dbsTask = oversightTask(2L, TaskType.TASK,
            "Supplier - Foo - DBS tilsyn", LocalDate.of(2026, 3, 22));
        final Asset asset = asset(dbsModel());
        when(relationService.findAllRelatedTo(asset)).thenReturn(List.<Relatable>of(oldCheck, dbsTask));

        final AssetOversight oversight = new AssetOversight();
        oversight.setAsset(asset);
        oversight.setSupervisionModel(dbsModel());

        final Task selected = assetOversightService.findTaskForOversightCompletion(oversight);

        assertThat(selected).isSameAs(dbsTask);
    }

    @Test
    void booksNonDbsOversightOntoCheckTaskWhenMultipleTasksExist() {
        final Task oldCheck = oversightTask(1L, TaskType.CHECK,
            "Udfør tilsyn med leverandøren af Foo", LocalDate.of(2026, 3, 1));
        final Task dbsTask = oversightTask(2L, TaskType.TASK,
            "Supplier - Foo - DBS tilsyn", LocalDate.of(2026, 3, 22));
        final Asset asset = asset(otherModel());
        when(relationService.findAllRelatedTo(asset)).thenReturn(List.<Relatable>of(oldCheck, dbsTask));

        final AssetOversight oversight = new AssetOversight();
        oversight.setAsset(asset);
        oversight.setSupervisionModel(otherModel());

        final Task selected = assetOversightService.findTaskForOversightCompletion(oversight);

        assertThat(selected).isSameAs(oldCheck);
    }

    @Test
    void movesMisbookedLogFromOldTaskToOpenDbsTask() {
        final Task oldCheck = oversightTask(1L, TaskType.CHECK,
            "Udfør tilsyn med leverandøren af Foo", LocalDate.of(2026, 3, 1));
        final Task dbsTask = oversightTask(2L, TaskType.TASK,
            "Supplier - Foo - DBS tilsyn", LocalDate.of(2026, 3, 22));
        dbsTask.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));

        // A completed tilsyn that was misbooked onto the old CHECK task.
        final TaskLog misbooked = new TaskLog();
        misbooked.setId(10L);
        misbooked.setName("Tilsyn udført");
        misbooked.setDocumentationLink("http://localhost/assets/" + ASSET_ID);
        misbooked.setCompleted(LocalDate.of(2026, 3, 20));
        misbooked.setTask(oldCheck);
        oldCheck.getLogs().add(misbooked);

        final Asset asset = asset(dbsModel());
        when(relationService.findAllRelatedTo(asset)).thenReturn(List.<Relatable>of(oldCheck, dbsTask));

        final int moved = assetOversightService.repairMisbookedDbsOversightLogs(asset);

        assertThat(moved).isEqualTo(1);
        // The move goes through a direct FK update (never a collection mutation, which would trigger
        // orphanRemoval and delete the log). See the DB-level test for the actual move semantics.
        verify(taskLogDao).reassignTask(10L, dbsTask);
    }

    @Test
    void doesNotTouchAlreadyCompletedDbsTask() {
        final Task oldCheck = oversightTask(1L, TaskType.CHECK,
            "Udfør tilsyn med leverandøren af Foo", LocalDate.of(2026, 3, 1));
        final Task dbsTask = oversightTask(2L, TaskType.TASK,
            "Supplier - Foo - DBS tilsyn", LocalDate.of(2026, 3, 22));
        dbsTask.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));

        // DBS task already has its own log — nothing should move (idempotency).
        final TaskLog existing = new TaskLog();
        existing.setName("Tilsyn udført");
        existing.setDocumentationLink("http://localhost/assets/" + ASSET_ID);
        existing.setCompleted(LocalDate.of(2026, 3, 21));
        existing.setTask(dbsTask);
        dbsTask.getLogs().add(existing);

        final TaskLog strayOnCheck = new TaskLog();
        strayOnCheck.setName("Tilsyn udført");
        strayOnCheck.setDocumentationLink("http://localhost/assets/" + ASSET_ID);
        strayOnCheck.setCompleted(LocalDate.of(2026, 3, 20));
        strayOnCheck.setTask(oldCheck);
        oldCheck.getLogs().add(strayOnCheck);

        final Asset asset = asset(dbsModel());
        lenient().when(relationService.findAllRelatedTo(asset)).thenReturn(List.<Relatable>of(oldCheck, dbsTask));

        final int moved = assetOversightService.repairMisbookedDbsOversightLogs(asset);

        assertThat(moved).isZero();
        verify(taskLogDao, never()).reassignTask(any(), any());
        assertThat(oldCheck.getLogs()).containsExactly(strayOnCheck);
        assertThat(dbsTask.getLogs()).containsExactly(existing);
    }
}

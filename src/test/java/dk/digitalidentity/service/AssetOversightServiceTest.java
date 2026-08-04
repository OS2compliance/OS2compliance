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

    /**
     * The DBS import appends a new oversight to the newest open DBS task and resets its deadline, so
     * the completion must land on that same task. Picking by deadline instead would send the
     * completion to the oldest, most overdue duplicate — leaving the task that actually carries the
     * oversight open, and the abandoned duplicates alive forever.
     */
    @Test
    void booksOntoNewestDbsTaskWhenSeveralDuplicatesAreOpen() {
        final Task abandonedDbsTask = oversightTask(1L, TaskType.TASK,
            "Supplier - Foo - DBS tilsyn", LocalDate.of(2025, 7, 1));
        abandonedDbsTask.setCreatedAt(LocalDateTime.of(2025, 6, 1, 0, 0));
        final Task liveDbsTask = oversightTask(2L, TaskType.TASK,
            "Supplier - Foo - DBS tilsyn", LocalDate.of(2026, 9, 2));
        liveDbsTask.setCreatedAt(LocalDateTime.of(2026, 1, 10, 0, 0));
        final Asset asset = asset(dbsModel());
        when(relationService.findAllRelatedTo(asset))
            .thenReturn(List.<Relatable>of(abandonedDbsTask, liveDbsTask));

        final AssetOversight oversight = new AssetOversight();
        oversight.setAsset(asset);
        oversight.setSupervisionModel(dbsModel());

        final Task selected = assetOversightService.findTaskForOversightCompletion(oversight);

        assertThat(selected).isSameAs(liveDbsTask);
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

    /**
     * The common shape in customer data: the old selector consistently booked onto the oldest DBS task,
     * so a tilsyn registered weeks after a newer task had been imported piled up on the older one while
     * the newer stood overdue. The older task keeps a log of its own here, so donating is safe.
     */
    @Test
    void movesMisbookedLogFromOlderDbsTaskWhenItKeepsALogOfItsOwn() {
        final Task olderDbsTask = oversightTask(1L, TaskType.TASK,
            "Supplier - Foo - DBS tilsyn", LocalDate.of(2026, 4, 2));
        olderDbsTask.setCreatedAt(LocalDateTime.of(2026, 3, 3, 9, 25));
        final Task newerDbsTask = oversightTask(2L, TaskType.TASK,
            "Supplier - Foo - DBS tilsyn", LocalDate.of(2026, 8, 6));
        newerDbsTask.setCreatedAt(LocalDateTime.of(2026, 7, 7, 16, 58));

        // Two oversights registered after the newer task existed, both booked onto the older one.
        olderDbsTask.getLogs().add(oversightLog(10L, olderDbsTask, LocalDate.of(2026, 7, 21)));
        olderDbsTask.getLogs().add(oversightLog(11L, olderDbsTask, LocalDate.of(2026, 7, 8)));

        final Asset asset = asset(dbsModel());
        when(relationService.findAllRelatedTo(asset)).thenReturn(List.<Relatable>of(olderDbsTask, newerDbsTask));

        final int moved = assetOversightService.repairMisbookedDbsOversightLogs(asset);

        // The newest misbooked log moves; the older task keeps the other one and stays completed.
        assertThat(moved).isEqualTo(1);
        verify(taskLogDao).reassignTask(10L, newerDbsTask);
        verify(taskLogDao, never()).reassignTask(11L, newerDbsTask);
    }

    /**
     * When the older DBS task holds only one log, moving it would empty it and turn a completed task
     * back into an open, overdue one. We cannot tell from the data whether its own tilsyn was ever
     * performed, so these are left for a human.
     */
    @Test
    void leavesOlderDbsTaskAloneWhenItWouldBeEmptied() {
        final Task olderDbsTask = oversightTask(1L, TaskType.TASK,
            "Supplier - Foo - DBS tilsyn", LocalDate.of(2026, 4, 2));
        olderDbsTask.setCreatedAt(LocalDateTime.of(2026, 3, 3, 9, 25));
        final Task newerDbsTask = oversightTask(2L, TaskType.TASK,
            "Supplier - Foo - DBS tilsyn", LocalDate.of(2026, 8, 6));
        newerDbsTask.setCreatedAt(LocalDateTime.of(2026, 7, 7, 16, 58));
        olderDbsTask.getLogs().add(oversightLog(10L, olderDbsTask, LocalDate.of(2026, 7, 21)));

        final Asset asset = asset(dbsModel());
        when(relationService.findAllRelatedTo(asset)).thenReturn(List.<Relatable>of(olderDbsTask, newerDbsTask));

        final int moved = assetOversightService.repairMisbookedDbsOversightLogs(asset);

        assertThat(moved).isZero();
        verify(taskLogDao, never()).reassignTask(any(), any());
    }

    /** A newer DBS task must never be robbed to fill an older, still-open one. */
    @Test
    void doesNotMoveLogFromNewerDbsTaskToOlderOpenTask() {
        final Task olderOpenDbsTask = oversightTask(1L, TaskType.TASK,
            "Supplier - Foo - DBS tilsyn", LocalDate.of(2026, 4, 2));
        olderOpenDbsTask.setCreatedAt(LocalDateTime.of(2026, 3, 3, 9, 25));
        final Task newerCompletedDbsTask = oversightTask(2L, TaskType.TASK,
            "Supplier - Foo - DBS tilsyn", LocalDate.of(2026, 8, 6));
        newerCompletedDbsTask.setCreatedAt(LocalDateTime.of(2026, 7, 7, 16, 58));
        newerCompletedDbsTask.getLogs().add(oversightLog(10L, newerCompletedDbsTask, LocalDate.of(2026, 7, 21)));
        newerCompletedDbsTask.getLogs().add(oversightLog(11L, newerCompletedDbsTask, LocalDate.of(2026, 7, 24)));

        final Asset asset = asset(dbsModel());
        when(relationService.findAllRelatedTo(asset))
            .thenReturn(List.<Relatable>of(olderOpenDbsTask, newerCompletedDbsTask));

        final int moved = assetOversightService.repairMisbookedDbsOversightLogs(asset);

        assertThat(moved).isZero();
        verify(taskLogDao, never()).reassignTask(any(), any());
    }

    private TaskLog oversightLog(final long id, final Task task, final LocalDate completed) {
        final TaskLog taskLog = new TaskLog();
        taskLog.setId(id);
        taskLog.setName("Tilsyn udført");
        taskLog.setDocumentationLink("http://localhost/assets/" + ASSET_ID);
        taskLog.setCompleted(completed);
        taskLog.setTask(task);
        return taskLog;
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

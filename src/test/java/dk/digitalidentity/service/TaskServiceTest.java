package dk.digitalidentity.service;

import dk.digitalidentity.Constants;
import dk.digitalidentity.dao.DocumentDao;
import dk.digitalidentity.dao.TaskDao;
import dk.digitalidentity.dao.TaskLogDao;
import dk.digitalidentity.dao.grid.TaskGridDao;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLog;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private DocumentDao documentDao;
    @Mock private TaskDao taskDao;
    @Mock private TaskLogDao taskLogDao;
    @Mock private TaskGridDao taskGridDao;

    @InjectMocks private TaskService taskService;

    private Task checkTask(final LocalDate deadline, final TaskRepetition repetition) {
        final Task task = new Task();
        task.setTaskType(TaskType.CHECK);
        task.setNextDeadline(deadline);
        task.setRepetition(repetition);
        return task;
    }

    private TaskLog completedOn(final LocalDate completed) {
        final TaskLog taskLog = new TaskLog();
        taskLog.setCompleted(completed);
        return taskLog;
    }

    @ParameterizedTest
    @EnumSource(value = TaskRepetition.class, names = {"YEARLY", "HALF_YEARLY", "QUARTERLY"})
    void onTimeCompletionAdvancesOneInterval(final TaskRepetition repetition) {
        final LocalDate deadline = intervalDeadline(repetition);
        final Task task = checkTask(deadline, repetition);

        taskService.completeTask(task, completedOn(deadline), null);

        assertThat(task.getNextDeadline()).isEqualTo(addInterval(deadline, repetition));
    }

    @ParameterizedTest
    @EnumSource(value = TaskRepetition.class, names = {"YEARLY", "HALF_YEARLY", "QUARTERLY"})
    void earlyCompletionStillAdvancesOneFullInterval(final TaskRepetition repetition) {
        final LocalDate deadline = intervalDeadline(repetition);
        final Task task = checkTask(deadline, repetition);

        taskService.completeTask(task, completedOn(deadline.minusDays(1)), null);

        assertThat(task.getNextDeadline()).isEqualTo(addInterval(deadline, repetition));
    }

    @ParameterizedTest
    @EnumSource(value = TaskRepetition.class, names = {"YEARLY", "HALF_YEARLY", "QUARTERLY"})
    void lateCompletionWithinOneIntervalDoesNotShiftRhythm(final TaskRepetition repetition) {
        final LocalDate deadline = intervalDeadline(repetition);
        final Task task = checkTask(deadline, repetition);
        final LocalDate completedLate = deadline.plusDays(20);

        taskService.completeTask(task, completedOn(completedLate), null);

        assertThat(task.getNextDeadline()).isEqualTo(addInterval(deadline, repetition));
    }

    @ParameterizedTest
    @EnumSource(value = TaskRepetition.class, names = {"YEARLY", "HALF_YEARLY", "QUARTERLY"})
    void completionMoreThanOneIntervalLateLandsInTheFuture(final TaskRepetition repetition) {
        final LocalDate deadline = intervalDeadline(repetition);
        final Task task = checkTask(deadline, repetition);
        // More than a full interval late: e.g. yearly deadline missed for over a year.
        final LocalDate completedVeryLate = addInterval(deadline, repetition).plusMonths(2);
        task.setNextDeadline(deadline);

        taskService.completeTask(task, completedOn(completedVeryLate), null);

        assertThat(task.getNextDeadline()).isAfter(completedVeryLate);
    }

    @ParameterizedTest
    @EnumSource(value = TaskRepetition.class, names = {"YEARLY", "HALF_YEARLY", "QUARTERLY"})
    void secondCompletionShortlyAfterFirstDoesNotMoveDeadlineAgain(final TaskRepetition repetition) {
        final LocalDate originalDeadline = intervalDeadline(repetition);
        final Task task = checkTask(originalDeadline, repetition);

        // First completion, on time: advances the deadline by one interval.
        taskService.completeTask(task, completedOn(originalDeadline), null);
        final LocalDate afterFirstCompletion = task.getNextDeadline();
        assertThat(afterFirstCompletion).isEqualTo(addInterval(originalDeadline, repetition));

        // Second completion shortly after, still inside the period that was just closed.
        final LocalDate secondCompletionDate = originalDeadline.minusDays(15);
        taskService.completeTask(task, completedOn(secondCompletionDate), null);

        assertThat(task.getNextDeadline()).isEqualTo(afterFirstCompletion);
    }

    @Test
    void noRepetitionLeavesDeadlineUnchanged() {
        final LocalDate deadline = LocalDate.of(2026, 6, 1);
        final Task task = checkTask(deadline, TaskRepetition.NONE);

        taskService.completeTask(task, completedOn(LocalDate.of(2026, 6, 1)), null);

        assertThat(task.getNextDeadline()).isEqualTo(deadline);
    }

    @Test
    void nullRepetitionLeavesDeadlineUnchanged() {
        final LocalDate deadline = LocalDate.of(2026, 6, 1);
        final Task task = checkTask(deadline, null);

        taskService.completeTask(task, completedOn(LocalDate.of(2026, 6, 1)), null);

        assertThat(task.getNextDeadline()).isEqualTo(deadline);
    }

    @Test
    void monthlyRepetitionDoesNotDriftOnShortMonths() {
        final LocalDate deadline = LocalDate.of(2026, 1, 31);
        final Task task = checkTask(deadline, TaskRepetition.MONTHLY);

        // Missed two months in a row: naive single-step chaining would clamp Jan 31 -> Feb 28 -> Mar 28.
        taskService.completeTask(task, completedOn(LocalDate.of(2026, 3, 15)), null);

        assertThat(task.getNextDeadline()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void manualOverrideBypassesComputation() {
        final LocalDate deadline = LocalDate.of(2026, 12, 1);
        final Task task = checkTask(deadline, TaskRepetition.YEARLY);
        final LocalDate override = LocalDate.of(2030, 1, 1);

        taskService.completeTask(task, completedOn(LocalDate.of(2026, 12, 1)), override);

        assertThat(task.getNextDeadline()).isEqualTo(override);
    }

    @Test
    void linkedDocumentNextRevisionFollowsCorrectedDeadline() {
        final LocalDate originalDeadline = LocalDate.of(2026, 12, 1);
        final Task task = checkTask(originalDeadline, TaskRepetition.YEARLY);
        final Document document = new Document();
        document.setNextRevision(originalDeadline);
        task.getProperties().add(Property.builder()
            .entity(task)
            .key(Constants.ASSOCIATED_DOCUMENT_PROPERTY)
            .value("42")
            .build());
        lenient().when(documentDao.findById(42L)).thenReturn(java.util.Optional.of(document));

        // First completion, on time: advances deadline (and the document) by one interval.
        taskService.completeTask(task, completedOn(originalDeadline), null);
        final LocalDate afterFirstCompletion = task.getNextDeadline();
        assertThat(document.getNextRevision()).isEqualTo(afterFirstCompletion);

        // Second completion shortly after, inside the period that was just closed: neither the task
        // deadline nor the document's revision date should move again.
        taskService.completeTask(task, completedOn(originalDeadline.minusDays(15)), null);

        assertThat(task.getNextDeadline()).isEqualTo(afterFirstCompletion);
        assertThat(document.getNextRevision()).isEqualTo(afterFirstCompletion);
    }

    private LocalDate intervalDeadline(final TaskRepetition repetition) {
        return LocalDate.of(2026, 12, 1);
    }

    private LocalDate addInterval(final LocalDate date, final TaskRepetition repetition) {
        return switch (repetition) {
            case QUARTERLY -> date.plusMonths(3);
            case HALF_YEARLY -> date.plusMonths(6);
            case YEARLY -> date.plusYears(1);
            default -> throw new IllegalArgumentException("Unsupported in test: " + repetition);
        };
    }
}

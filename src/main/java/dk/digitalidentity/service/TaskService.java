package dk.digitalidentity.service;

import dk.digitalidentity.dao.DocumentDao;
import dk.digitalidentity.dao.TaskDao;
import dk.digitalidentity.dao.TaskLogDao;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLog;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.model.TaskDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.ASSOCIATED_DOCUMENT_PROPERTY;
import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {
    private final DocumentDao documentDao;
    private final TaskDao taskDao;
    private final TaskLogDao taskLogDao;
	private final SettingsService settingsService;
    private final RelationService relationService;

    public List<Task> findAll() {
        return taskDao.findAll();
    }


    public void saveAll(final List<Task> all) {
        taskDao.saveAll(all);
    }

    public Optional<Task> findById(final Long id) {
        return taskDao.findById(id);
    }

    public List<Task> findRelatedTasks(final Relatable relatable, final Predicate<Task> taskPredicate) {
        final List<Relation> relations = relationService.findRelatedToWithType(relatable, RelationType.TASK);
        return relations.stream()
            .map(r -> r.getRelationAType().equals(RelationType.TASK) ? r.getRelationAId() : r.getRelationBId())
            .map(taskDao::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(taskPredicate)
            .toList();
    }

    public List<Task> findTaskWithProperty(final String propertyName, final String propertyValue) {
        return taskDao.findByProperty(propertyName, propertyValue);
    }

    @Transactional
    public List<Task> allTasksWithTag(final Long tagId) {
        return taskDao.findByTag(tagId);
    }

    @Transactional
    public List<Task> findTasksThatNeedsNotification() {
        final LocalDate date = closeToDeadline();
        return taskDao.findByNotifyResponsibleTrueAndNextDeadlineBeforeAndHasNotifiedResponsibleFalse(date);
    }

    public List<Task> findAllTasksWithDeadlineAfter(final LocalDate date) {
        return taskDao.findByNextDeadlineAfterOrderByNextDeadlineAsc(date);
    }

    public Task copyTask(final Task oldTask) {
        final Task task = new Task();
        task.setName(oldTask.getName());
        task.setTaskType(oldTask.getTaskType());
        task.setNextDeadline(oldTask.getNextDeadline());
        task.setResponsibleUser(oldTask.getResponsibleUser());
        task.setResponsibleOu(oldTask.getResponsibleOu());
        task.setRepetition(oldTask.getRepetition());
        task.setTags(oldTask.getTags());
        task.setDescription(oldTask.getDescription());
        task.setCreatedAt(LocalDateTime.now());
        task.setCreatedBy(SecurityUtil.getLoggedInUserUuid());
        task.setIncludeInReport(oldTask.getIncludeInReport());

        return taskDao.save(task);
    }

    @Transactional
    public Task saveTask(final Task task) {
        return taskDao.save(task);
    }

    @Transactional
    public void completeTask(final Task task, final TaskLog taskLog) {
        task.getLogs().add(taskLog);
        if (task.getTaskType() == TaskType.CHECK) {
            final LocalDate nextDeadline = getNextDeadline(task.getNextDeadline(), task.getRepetition());
            // Check if we need to move date on related assets
            task.getProperties().stream()
                .filter(p -> p.getKey().equals(ASSOCIATED_DOCUMENT_PROPERTY))
                .findFirst()
                .flatMap(property ->
                    documentDao.findById(Long.parseLong(property.getValue())))
                .ifPresent(d -> {
                    if (d.getNextRevision().isEqual(task.getNextDeadline())) {
                        d.setNextRevision(nextDeadline);
                    }
                });
            task.setNextDeadline(nextDeadline);
        }
    }

    public boolean isTaskDone(final Task task) {
        if (task.getLogs().isEmpty()) {
            return false;
        }
        return task.getTaskType() == TaskType.TASK;
    }

    public List<TaskDTO> buildRelatedTasks(final List<ThreatAssessment> threatAssessments, final boolean onlyNotCompleted) {
        return threatAssessments.stream()
            .flatMap(a -> buildRelatedTasks(a, onlyNotCompleted).stream())
            .collect(Collectors.toList());
    }

    public List<TaskDTO> buildRelatedTasks(final ThreatAssessment threatAssessment, final boolean onlyNotCompleted) {
        final List<TaskDTO> relatedTasks = new ArrayList<>();
        final List<Relatable> tasks = relationService.findAllRelatedTo(threatAssessment).stream().filter(r -> r.getRelationType() == RelationType.TASK).toList();
        for (final Relatable taskAsRelatable : tasks) {
            final Task task = (Task) taskAsRelatable;
            if (onlyNotCompleted && task.getTaskType().equals(TaskType.TASK) && !task.getLogs().isEmpty()) {
                continue;
            }
            relatedTasks.add(new TaskDTO(task.getId(), task.getName(), task.getTaskType(), task.getResponsibleUser().getName(), task.getNextDeadline().format(DK_DATE_FORMATTER), task.getNextDeadline().isBefore(LocalDate.now())));
        }
        return relatedTasks;
    }

    private LocalDate closeToDeadline() {
        return LocalDate.now().plusDays(settingsService.getInt("notify.days",10));
    }

    @Transactional
    public void deleteAll(final List<Task> tasks) {
        taskDao.deleteAll(tasks);
    }

    @Transactional
    public void deleteById(final Long taskId) {
        taskDao.deleteById(taskId);
    }

    public List<TaskLog> logsBetween(final Task task, final LocalDate from, final LocalDate to) {
        return taskLogDao.findAllByTaskFiltered(task, from, to);
    }

    private LocalDate getNextDeadline(final LocalDate deadline, final TaskRepetition repetition) {
        if (repetition == null) {
            return deadline;
        }
        return switch (repetition) {
            case MONTHLY -> deadline.plusMonths(1);
            case QUARTERLY -> deadline.plusMonths(3);
            case HALF_YEARLY -> deadline.plusMonths(6);
            case YEARLY -> deadline.plusYears(1);
            case EVERY_SECOND_YEAR -> deadline.plusYears(2);
            case EVERY_THIRD_YEAR -> deadline.plusYears(3);
            default -> deadline;
        };
    }

}

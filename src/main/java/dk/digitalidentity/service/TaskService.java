package dk.digitalidentity.service;

import dk.digitalidentity.dao.TaskDao;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.model.TaskDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;

@Service
@Slf4j
public class TaskService {
 	@Autowired
	private TaskDao taskDao;
	@Autowired
	private SettingsService settingsService;
    @Autowired
    private RelationService relationService;

    public Optional<Task> findById(final Long id) {
        return taskDao.findById(id);
    }

    @Transactional
    public List<Task> findTasksNearingDeadlineForUser(final User user) {
        return taskDao.findByResponsibleUserAndNextDeadlineBefore(user, closeToDeadline());
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

    public Task copyTask(Task oldTask) {
        Task task = new Task();
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


        return taskDao.save(task);
    }

    @Transactional
    public Task createTask(final Task task) {
        return taskDao.save(task);
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

}

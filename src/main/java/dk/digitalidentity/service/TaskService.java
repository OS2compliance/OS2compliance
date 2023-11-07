package dk.digitalidentity.service;

import dk.digitalidentity.dao.TaskDao;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.service.model.TaskDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;

@Service
@Slf4j
public class TaskService {
 	@Autowired
	TaskDao taskDao;
	@Autowired
	private SettingsService settingsService;
    @Autowired
    private RelationService relationService;

	@Transactional
	public List<Task> findTasksNearingDeadlines(){
		return findTasksNearingDeadlines(false, null);
	}

    @Transactional
    public List<Task> findTasksNearingDeadlines(final User user){
        return findTasksNearingDeadlines(false, user);
    }

    @Transactional
    public List<Task> findTasksNearingDeadlines(final boolean shouldNotify){
        return findTasksNearingDeadlines(shouldNotify, null);
    }
    @Transactional
    public List<Task> findTasksNearingDeadlines(Boolean shouldNotify, @Nullable final User user){
        try{
            final LocalDate date = LocalDate.now().plusDays(settingsService.getInt("notify.days",10));
            if(shouldNotify = false){
                return taskDao.findByNextDeadlineBefore(date).get();
            } else if(user != null) {
                return taskDao.findByResponsibleUserAndNextDeadlineBefore(user, date).get();
            } else {
                return taskDao.findByNotifyResponsibleTrueAndNextDeadlineBeforeAndHasNotifiedResponsibleFalse(date).get();
            }

        }
        catch (final NoSuchElementException e) {
            return List.of();
        }
    }

    @Transactional
    public Task createTask(final Task task) {
        return taskDao.save(task);
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
}

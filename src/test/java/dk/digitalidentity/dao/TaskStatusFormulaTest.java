package dk.digitalidentity.dao;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLog;
import dk.digitalidentity.model.entity.enums.TaskDeadlineStatus;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class TaskStatusFormulaTest extends BaseIntegrationTest {

	@Autowired private TaskDao taskDao;
	@Autowired private TaskLogDao taskLogDao;
	@PersistenceContext private EntityManager em;

	@Test
	public void onlyOneOffTasksAreCompletedByACompletion() {
		final Long check = completedOnce(TaskType.CHECK, TaskRepetition.YEARLY);
		final Long task = completedOnce(TaskType.TASK, TaskRepetition.NONE);
		em.flush();
		em.clear();

		assertThat(taskDao.findById(check).orElseThrow().getStatus()).isEqualTo(TaskDeadlineStatus.FUTURE);
		assertThat(taskDao.findById(task).orElseThrow().getStatus()).isEqualTo(TaskDeadlineStatus.COMPLETED);
	}

	private Long completedOnce(final TaskType type, final TaskRepetition repetition) {
		final Task task = new Task();
		task.setTaskType(type);
		task.setRepetition(repetition);
		task.setName("Udført én gang");
		task.setNextDeadline(LocalDate.now().plusMonths(6));
		taskDao.save(task);

		final TaskLog log = new TaskLog();
		log.setName("Udført");
		log.setComment("");
		log.setResponsibleUserUserId("bruger");
		log.setDeadline(LocalDate.now().minusMonths(6));
		log.setCompleted(LocalDate.now().minusMonths(6));
		log.setTask(task);
		taskLogDao.save(log);
		return task.getId();
	}
}

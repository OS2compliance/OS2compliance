package dk.digitalidentity.service;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.TaskDao;
import dk.digitalidentity.dao.TaskLogDao;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLog;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DB-level test of the lower bound the year wheel projects repeating tasks back to: the oldest deadline
 * found on the task's logs, or nothing at all when the task has never been completed.
 */
@Transactional
public class YearWheelFirstDeadlineIntegrationTest extends BaseIntegrationTest {

	@Autowired private TaskService taskService;
	@Autowired private TaskDao taskDao;
	@Autowired private TaskLogDao taskLogDao;
	@PersistenceContext private EntityManager em;

	@Test
	public void neverCompletedTaskHasNoFirstDeadline() {
		final Task task = saveCheck("Årlig opdatering behandlingsaktivitet", LocalDate.of(2027, 8, 12));
		em.flush();

		assertThat(taskService.getFirstDeadlines(List.of(task.getId()))).isEmpty();
	}

	@Test
	public void firstDeadlineIsTheOldestLoggedDeadline() {
		final Task task = saveCheck("Årlig kontrol", LocalDate.of(2026, 8, 12));
		saveLog(task, LocalDate.of(2025, 8, 12), LocalDate.of(2025, 8, 15));
		saveLog(task, LocalDate.of(2024, 8, 12), LocalDate.of(2024, 8, 20));
		em.flush();

		assertThat(taskService.getFirstDeadlines(List.of(task.getId())))
				.containsExactly(Map.entry(task.getId(), LocalDate.of(2024, 8, 12)));
	}

	@Test
	public void firstDeadlinesAreLookedUpPerTask() {
		final Task completed = saveCheck("Årlig kontrol", LocalDate.of(2026, 8, 12));
		saveLog(completed, LocalDate.of(2025, 8, 12), LocalDate.of(2025, 8, 15));
		final Task fresh = saveCheck("Ny årlig kontrol", LocalDate.of(2027, 8, 12));
		em.flush();

		assertThat(taskService.getFirstDeadlines(List.of(completed.getId(), fresh.getId())))
				.containsExactly(Map.entry(completed.getId(), LocalDate.of(2025, 8, 12)));
	}

	@Test
	public void noTasksMeansNoLookup() {
		assertThat(taskService.getFirstDeadlines(List.of())).isEmpty();
	}

	private Task saveCheck(final String name, final LocalDate nextDeadline) {
		final Task task = new Task();
		task.setTaskType(TaskType.CHECK);
		task.setRepetition(TaskRepetition.YEARLY);
		task.setName(name);
		task.setNextDeadline(nextDeadline);
		return taskDao.save(task);
	}

	private void saveLog(final Task task, final LocalDate deadline, final LocalDate completed) {
		final TaskLog log = new TaskLog();
		log.setName("Kontrol udført");
		log.setComment("");
		log.setResponsibleUserUserId("kbp");
		log.setDeadline(deadline);
		log.setCompleted(completed);
		log.setTask(task);
		taskLogDao.save(log);
	}
}

package dk.digitalidentity.dao;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class TaskDaoReminderTest extends BaseIntegrationTest {

	@Autowired private TaskDao taskDao;

	@Test
	public void noReminderBeforeTheTaskHasStarted() {
		final LocalDate deadline = LocalDate.now().plusDays(7);
		saveTask("Startet", LocalDate.now(), deadline);
		saveTask("Ikke startet", LocalDate.now().plusDays(1), deadline);

		assertThat(taskDao.findWithoutReminderConfigDueOn(deadline))
				.extracting(Task::getName)
				.containsExactly("Startet");
	}

	private void saveTask(final String name, final LocalDate startDate, final LocalDate deadline) {
		final Task task = new Task();
		task.setTaskType(TaskType.CHECK);
		task.setRepetition(TaskRepetition.YEARLY);
		task.setName(name);
		task.setNotifyResponsible(true);
		task.setStartDate(startDate);
		task.setNextDeadline(deadline);
		taskDao.save(task);
	}
}

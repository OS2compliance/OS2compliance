package dk.digitalidentity.dao;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class TaskStartDateTest extends BaseIntegrationTest {

	@Autowired private TaskDao taskDao;
	@Autowired private EntityManager entityManager;

	@Test
	public void startDateFollowsDeadlineMovedBeforeIt() {
		final Task task = new Task();
		task.setTaskType(TaskType.CHECK);
		task.setRepetition(TaskRepetition.YEARLY);
		task.setName("Revisionskontrol");
		task.setStartDate(LocalDate.now());
		task.setNextDeadline(LocalDate.now().plusMonths(1));
		taskDao.saveAndFlush(task);

		final LocalDate earlierDeadline = LocalDate.now().minusMonths(1);
		task.setNextDeadline(earlierDeadline);
		taskDao.saveAndFlush(task);
		entityManager.clear();

		assertThat(taskDao.findById(task.getId()).orElseThrow().getStartDate()).isEqualTo(earlierDeadline);
	}
}

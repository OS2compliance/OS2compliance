package dk.digitalidentity.dao.grid;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.TaskDao;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.grid.TaskGrid;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class TaskGridStatusTest extends BaseIntegrationTest {

	@Autowired private TaskDao taskDao;
	@Autowired private TaskGridDao taskGridDao;
	@PersistenceContext private EntityManager em;

	@BeforeEach
	public void relaxGroupBy() {
		// The view relies on MariaDB's lenient GROUP BY, which the MySQL test container rejects
		em.createNativeQuery("SET SESSION sql_mode = REPLACE(@@sql_mode, 'ONLY_FULL_GROUP_BY', '')").executeUpdate();
	}

	@Test
	public void overdueTaskInProgressIsFilteredAsInProgress() {
		saveOverdueTask("Overskredet i gang", true);
		saveOverdueTask("Overskredet", false);
		em.flush();

		assertThat(findByStatus("IN_PROGRESS")).containsExactly("Overskredet i gang");
		assertThat(findByStatus("EXCEEDED")).containsExactly("Overskredet");
	}

	private List<String> findByStatus(final String status) {
		return taskGridDao.findAllWithColumnSearch(Map.of("taskDeadlineStatus", status, "name", "Overskredet"),
				PageRequest.of(0, 50), TaskGrid.class).map(TaskGrid::getName).getContent();
	}

	private void saveOverdueTask(final String name, final boolean inProgress) {
		final Task task = new Task();
		task.setTaskType(TaskType.TASK);
		task.setRepetition(TaskRepetition.NONE);
		task.setName(name);
		task.setNextDeadline(LocalDate.now().minusDays(1));
		task.setInProgress(inProgress);
		taskDao.save(task);
	}
}

package dk.digitalidentity.service;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.OrganisationUnitDao;
import dk.digitalidentity.dao.TaskDao;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.TaskType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
@Transactional
public class TaskDaoTest extends BaseIntegrationTest {

	@Autowired
	private TaskDao taskDao;
	@Autowired
	private TaskService taskService;
	@Autowired
	private UserDao userDao;
	@Autowired
	private OrganisationUnitDao organisationUnitDao;

	@Test
	public void canFindTasksWithDeadlineClosingInAndNotifyIsTrue() {
		final User user = createUser("ff6fc101-aeb2-486e-8d39-5d8e718abdec", "and", "Andreas Patrick Duffy");
		final OrganisationUnit ou = createOU();
		//should be there
		final var taskCloseDeadline = createTestTask(LocalDate.now().plusDays(5), true, user, ou);
		final var taskCloseDeadline2 = createTestTask(LocalDate.now().plusDays(7), true, user, ou);
		final var taskCloseDeadline3 = createTestTask(LocalDate.now().plusDays(9), true, user, ou);

		//should not be there
		final var taskNotCloseDeadline = createTestTask(LocalDate.now().plusYears(10), false, user, ou);
		final var taskNotCloseDeadlineButTrue = createTestTask(LocalDate.now().plusYears(10), true, user, ou);


		//when
		final List<Task> tasksCloseToDeadline = taskService.findTasksNearingDeadlines(true);


		//then
		assertThat(tasksCloseToDeadline)
				.hasSize(3)
				.extracting(Task::getNextDeadline)
				.containsExactlyInAnyOrder(LocalDate.now().plusDays(5),LocalDate.now().plusDays(7),LocalDate.now().plusDays(9));

	}



	private Task createTestTask(final LocalDate deadline, final boolean shouldNotify, final User user, final OrganisationUnit ou){
		final var task = new Task();
		task.setName("Name");
		task.setTaskType(TaskType.TASK);
		task.setDescription("Kaspar får os til at lave Tests :(");
		task.setResponsibleUser(user);
		task.setResponsibleOu(ou);
		task.setNotifyResponsible(shouldNotify);
		task.setHasNotifiedResponsible(false);
		task.setNextDeadline(deadline);
		return taskDao.save(task);
	}
	private OrganisationUnit createOU() {
		final OrganisationUnit ou = new OrganisationUnit();
		ou.setUuid(String.valueOf(UUID.randomUUID()));
		ou.setName("Test");
		ou.setActive(true);
		return organisationUnitDao.save(ou);
	}

	private User createUser(final String uuid, final String initials, final String name) {
		 return userDao.save(User.builder()
				.uuid(uuid)
				.userId(initials)
				.name(name)
				.build());
	}

}

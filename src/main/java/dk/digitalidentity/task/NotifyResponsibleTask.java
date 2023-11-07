package dk.digitalidentity.task;

import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.service.MailService;
import dk.digitalidentity.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@EnableScheduling
public class NotifyResponsibleTask {
	@Autowired
	private MailService mailService;
	@Autowired
	private TaskService taskService;
	@Autowired
	private Environment environment;
	@Autowired
	private UserDao userDao;

	//04:05 hver dag
	@Scheduled(cron = "${os2compliance.mail.cron}")
	@Transactional
	public void notifyResponsibleUsersAboutDeadline() {
		final LocalDate today = LocalDate.now();
		final var users = userDao.findAll();

		final List<Task> tasks = taskService.findTasksNearingDeadlines(true);
		if (tasks.size() > 0) {
			for (final Task t : tasks) {
				final String msg = "Din opgave " + t.getName() + " har deadline om " + ChronoUnit.DAYS.between(today, t.getNextDeadline()) + " dag(e).\n" +
						" Du kan finde opgaven her: " + environment.getProperty("di.saml.sp.baseUrl") + "/tasks/" +  t.getId();
				 if(mailService.sendMessage(t.getResponsibleUser().getEmail(), "Deadline Notification", msg)) {
					t.setHasNotifiedResponsible(true);
				}
			}
		}
	}
}

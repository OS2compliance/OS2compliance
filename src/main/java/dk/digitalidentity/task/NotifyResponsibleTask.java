package dk.digitalidentity.task;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.service.NotifyService;
import dk.digitalidentity.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class NotifyResponsibleTask {
	private final TaskService taskService;
    private final NotifyService notifyService;
    private final OS2complianceConfiguration configuration;

    //04:05 hver dag
	@Scheduled(cron = "${os2compliance.mail.cron}")
	public void notifyResponsibleUsersAboutDeadline() {
        if (!configuration.isSchedulingEnabled()) {
            return;
        }
		taskService.findTasksThatNeedsNotification()
            .forEach(taskId -> notifyService.notifyTask(taskId.getId()));
	}
}

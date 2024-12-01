package dk.digitalidentity.task;

import dk.digitalidentity.config.GRComplianceConfiguration;
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
    private final GRComplianceConfiguration configuration;

    //04:05 hver dag
	@Scheduled(cron = "${grcompliance.mail.cron}")
	public void notifyResponsibleUsersAboutDeadline() {
        if (!configuration.isSchedulingEnabled()) {
            return;
        }
		taskService.findTasksThatNeedsNotification()
            .forEach(taskId -> notifyService.notifyTask(taskId.getId()));
	}
}

package dk.digitalidentity.task;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.model.entity.Setting;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.enums.NotificationSetting;
import dk.digitalidentity.service.NotifyService;
import dk.digitalidentity.service.SettingsService;
import dk.digitalidentity.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static dk.digitalidentity.Constants.LAST_NOTIFY_RUN_DAY_SETTING;


@Component
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class NotifyResponsibleTask {
    private final TaskService taskService;
    private final NotifyService notifyService;
    private final SettingsService settingsService;
    private final OS2complianceConfiguration configuration;

    //04:05 hver dag
	@Scheduled(cron = "${os2compliance.mail.cron}")
	public void notifyResponsibleUsersAboutDeadline() {
        if (!configuration.isSchedulingEnabled()) {
            return;
        }
        final ZonedDateTime lastRun = settingsService.getZonedDateTime(LAST_NOTIFY_RUN_DAY_SETTING, LocalDate.ofEpochDay(0).atStartOfDay(ZoneId.systemDefault()));
        if (lastRun.toLocalDate().isEqual(LocalDate.now())) {
            // Guard against running multiple times a day
            return;
        } else {
            settingsService.setZonedDateTime(LAST_NOTIFY_RUN_DAY_SETTING, ZonedDateTime.now());
        }
		log.info("Start: Notifying responsible users about deadlines");

        //Get notification settings
        List<Setting> notificationSettings = settingsService.getByAssociation("notification");


        for (Setting setting : notificationSettings) {
			try {
				NotificationSetting notificationSetting = NotificationSetting.fromValue(setting.getSettingKey());
				if (notificationSetting != null && setting.getSettingValue().equalsIgnoreCase("true")) {
					switch (notificationSetting) {
						case ONEMONTHBEFORE -> {
							taskService.getTasksWithDeadLineAtAndTaskNotificationOverrideFalse(LocalDate.now().plusMonths(1))
									.forEach(taskId -> notifyService.notifyTask(taskId.getId()));
						}
						case SEVENDAYSBEFORE -> {
							taskService.getTasksWithDeadLineAtAndTaskNotificationOverrideFalse(LocalDate.now().plusDays(7))
									.forEach(taskId -> notifyService.notifyTask(taskId.getId()));
						}
						case ONEDAYBEFORE -> {
							taskService.getTasksWithDeadLineAtAndTaskNotificationOverrideFalse(LocalDate.now().plusDays(1))
									.forEach(taskId -> notifyService.notifyTask(taskId.getId()));
						}
						case ONDAY -> {
							taskService.getTasksWithDeadLineAtAndTaskNotificationOverrideFalse(LocalDate.now())
									.forEach(taskId -> notifyService.notifyTask(taskId.getId()));
						}
						case EVERYSEVENDAYSAFTER -> {
							LocalDate currentDate = LocalDate.now();
							LocalDate threeMonthsBefore = currentDate.minusMonths(3);
							List<LocalDate> sevenMultipleDates = new ArrayList<>();

							currentDate = currentDate.minusDays(7);
							while (currentDate.isAfter(threeMonthsBefore)) {
								sevenMultipleDates.add(currentDate);
								currentDate = currentDate.minusDays(7);
							}

							taskService.getTasksWithDeadLineInAndTaskNotificationOverrideFalse(sevenMultipleDates)
									.forEach(taskId -> notifyService.notifyTask(taskId.getId()));
						}
						default -> {
							log.warn("Unknown notification setting: " + setting.getSettingKey().toUpperCase());
						}
					}
				}
			} catch (IllegalArgumentException e) {
				log.warn("Illegal argument for notification setting option: " + setting.getSettingKey().toUpperCase() + ": \n" + e.getMessage());
			}
        }
		// Handle tasks with custom notification settings
		for (NotificationSetting notificationSetting : NotificationSetting.values()) {
			List<Task> tasksToNotify = new ArrayList<>();

			switch (notificationSetting) {
				case ONEMONTHBEFORE:
					tasksToNotify = taskService.getTasksWithDeadlineAtAndNotificationSettingContains(
							LocalDate.now().plusMonths(1), notificationSetting);
					break;
				case SEVENDAYSBEFORE:
					tasksToNotify = taskService.getTasksWithDeadlineAtAndNotificationSettingContains(
							LocalDate.now().plusDays(7), notificationSetting);
					break;

				case ONEDAYBEFORE:
					tasksToNotify = taskService.getTasksWithDeadlineAtAndNotificationSettingContains(
							LocalDate.now().plusDays(1), notificationSetting);
					break;

				case ONDAY:
					tasksToNotify = taskService.getTasksWithDeadlineAtAndNotificationSettingContains(
							LocalDate.now(), notificationSetting);
					break;

				case EVERYSEVENDAYSAFTER:
					LocalDate currentDate = LocalDate.now();
					LocalDate threeMonthsBefore = currentDate.minusMonths(3);
					List<LocalDate> sevenMultipleDates = new ArrayList<>();

					currentDate = currentDate.minusDays(7);
					while (currentDate.isAfter(threeMonthsBefore)) {
						sevenMultipleDates.add(currentDate);
						currentDate = currentDate.minusDays(7);
					}

					tasksToNotify = taskService.getTasksWithDeadlineInAndNotificationSettingContains(
							sevenMultipleDates, notificationSetting);
					break;
			}
			tasksToNotify.forEach(task -> notifyService.notifyTask(task.getId()));
		}
		log.info("Done: Notifying responsible users about deadlines");
	}
}

package dk.digitalidentity.task;

import dk.digitalidentity.config.GRComplianceConfiguration;
import dk.digitalidentity.model.entity.Setting;
import dk.digitalidentity.model.entity.enums.NotificationSetting;
import dk.digitalidentity.service.NotifyService;
import dk.digitalidentity.service.SettingsService;
import dk.digitalidentity.service.TaskService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class NotifyResponsibleTask {
    private final TaskService taskService;
    private final NotifyService notifyService;
    private final SettingsService settingsService;
    private final GRComplianceConfiguration configuration;

    //04:05 hver dag
	@Scheduled(cron = "${grcompliance.mail.cron}")
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

        //Get notification settings
        List<Setting> notificationSettings = settingsService.getByAssociation("notification");


        for (Setting setting : notificationSettings) {
            if (setting.getSettingValue().equalsIgnoreCase("true")) {
                if (setting.getSettingKey().equalsIgnoreCase(NotificationSetting.SEVENDAYSBEFORE.getValue())) {
                    taskService.getTasksWithDeadLineAt(LocalDate.now().plusDays(7))
                        .forEach(taskId -> notifyService.notifyTask(taskId.getId()));
                } else if (setting.getSettingKey().equalsIgnoreCase(NotificationSetting.ONEDAYBEFORE.getValue())) {
                    taskService.getTasksWithDeadLineAt(LocalDate.now().plusDays(1))
                        .forEach(taskId -> notifyService.notifyTask(taskId.getId()));
                } else if (setting.getSettingKey().equalsIgnoreCase(NotificationSetting.ONDAY.getValue())) {
                    taskService.getTasksWithDeadLineAt(LocalDate.now())
                        .forEach(taskId -> notifyService.notifyTask(taskId.getId()));
                } else if (setting.getSettingKey().equalsIgnoreCase(NotificationSetting.EVERYSEVENDAYSAFTER.getValue())) {
                    LocalDate currentDate = LocalDate.now(); //holds the current date, mutated by the loop below
                    LocalDate threeMonthsBefore = currentDate.minusMonths(3); // maximum timeframe to notify
                    List<LocalDate> sevenMultipleDates = new ArrayList<>(); //holds dates that are multiples of seven

                    currentDate = currentDate.minusDays(7); // start 7 days before today
                    while (currentDate.isAfter(threeMonthsBefore)) { //while the current date is within the max timeframe
                        sevenMultipleDates.add(currentDate); //add to list of dates
                        currentDate = currentDate.minusDays(7); //subtract 7 days
                    }

                    //Notify all matching tasks
                    taskService.getTasksWithDeadLineIn(sevenMultipleDates).forEach(taskId -> notifyService.notifyTask(taskId.getId()));


                }
            }
        }
    }
}

package dk.digitalidentity.service;

import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.samlmodule.config.settings.DISAML_Configuration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class NotifyService {
    private final DISAML_Configuration diSamlConfiguration;
    private final MailService mailService;
    private final TaskService taskService;

    public NotifyService(final DISAML_Configuration diSamlConfiguration, final MailService mailService, final TaskService taskService) {
        this.diSamlConfiguration = diSamlConfiguration;
        this.mailService = mailService;
        this.taskService = taskService;
    }

    @Transactional
    public void notifyTask(final Long taskId) {
        final Task task = taskService.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task with id: " + taskId + " not found"));
        if (task.getHasNotifiedResponsible() != null && task.getHasNotifiedResponsible()) {
            log.warn("Task '" + task.getName() + "' already notified");
            return;
        }
        final LocalDate today = LocalDate.now();
        final String baseUrl = diSamlConfiguration.getSp().getBaseUrl();
        final String msg = "Din opgave " + task.getName() + " har deadline om " + ChronoUnit.DAYS.between(today, task.getNextDeadline()) + " dag(e).\n" +
            " Du kan finde opgaven her: " + baseUrl + "/tasks/" +  task.getId();
        if (mailService.sendMessage(task.getResponsibleUser().getEmail(), "Deadline Notifikation", msg)) {
            task.setHasNotifiedResponsible(true);
        }
    }

}

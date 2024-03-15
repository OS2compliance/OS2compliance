package dk.digitalidentity.service;

import dk.digitalidentity.event.EmailEvent;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.samlmodule.config.settings.DISAML_Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyService {
    private final DISAML_Configuration diSamlConfiguration;
    private final TaskService taskService;
    private final ApplicationEventPublisher eventPublisher;

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
        task.setHasNotifiedResponsible(true);

        eventPublisher.publishEvent(EmailEvent.builder()
                .email(task.getResponsibleUser().getEmail())
                .subject("Deadline Notifikation")
                .message(msg)
            .build());
    }

}

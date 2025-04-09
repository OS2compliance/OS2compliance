package dk.digitalidentity.service;

import dk.digitalidentity.config.GRComplianceConfiguration;
import dk.digitalidentity.event.EmailEvent;
import dk.digitalidentity.model.entity.EmailTemplate;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.model.entity.enums.EmailTemplateType;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.view.ResponsibleUserView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyService {
    private final TaskService taskService;
    private final ApplicationEventPublisher eventPublisher;
    private final ResponsibleUserViewService responsibleUserViewService;
    private final SettingsService settingsService;
    private final EmailTemplateService emailTemplateService;
    private final GRComplianceConfiguration configuration;

    @Transactional
    public void notifyTask(final Long taskId) {
        final Task task = taskService.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task with id: " + taskId + " not found"));
        if (task.getTaskType() == TaskType.TASK && !task.getLogs().isEmpty()) {
            // Do not notify task already done
            return;
        }


        EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.TASK_REMINDER);
        if (template.isEnabled()) {
            final LocalDate today = LocalDate.now();
            final String baseUrl = configuration.getBaseUrl();
            final String url = baseUrl + "/tasks/" +  task.getId();
            final String link = "<a href=\"" + url + "\">" + url + "</a>";
            final String recipient = task.getResponsibleUser().getName();
            final String objectName = task.getName();
            final long days = ChronoUnit.DAYS.between(today, task.getNextDeadline());

            String title = template.getTitle();
            title = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient);
            title = title.replace(EmailTemplatePlaceholder.OBJECT_PLACEHOLDER.getPlaceholder(), objectName);
            title = title.replace(EmailTemplatePlaceholder.LINK_PLACEHOLDER.getPlaceholder(), link);
            title = title.replace(EmailTemplatePlaceholder.DAYS_TILL_DEADLINE.getPlaceholder(), Long.toString(days));
            String message = template.getMessage();
            message = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient);
            message = message.replace(EmailTemplatePlaceholder.OBJECT_PLACEHOLDER.getPlaceholder(), objectName);
            message = message.replace(EmailTemplatePlaceholder.LINK_PLACEHOLDER.getPlaceholder(), link);
            message = message.replace(EmailTemplatePlaceholder.DAYS_TILL_DEADLINE.getPlaceholder(), Long.toString(days));
            eventPublisher.publishEvent(EmailEvent.builder()
                .message(message)
                .subject(title)
                .email(task.getResponsibleUser().getEmail())
                .build());
        } else {
            log.info("Email template with type " + template.getTemplateType() + " is disabled. Email was not sent.");
        }
    }

    public void notifyAboutInactiveUsers(Set<String> newlyInactiveUuids) {
        if (!newlyInactiveUuids.isEmpty()) {
            String email = settingsService.getString("inactiveResponsibleEmail", null);
            if (StringUtils.hasLength(email)) {
                List<ResponsibleUserView> responsibleUsers = responsibleUserViewService.findAllIn(newlyInactiveUuids);
                if (!responsibleUsers.isEmpty()) {
                    EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.TASK_REMINDER);
                    if (template.isEnabled()) {
                        final String baseUrl = configuration.getBaseUrl();
                        final String url = baseUrl + "/admin/inactive";
                        final String link = "<a href=\"" + url + "\">" + url + "</a>";
                        String userList = "<ul>";

                        for (ResponsibleUserView responsibleUser : responsibleUsers) {
                            userList += "<li>" + responsibleUser.getName() + "(" + responsibleUser.getUserId() + ")</li>";
                        }

                        userList += "</ul>";

                        String title = template.getTitle();
                        title = title.replace(EmailTemplatePlaceholder.LINK_PLACEHOLDER.getPlaceholder(), link);
                        title = title.replace(EmailTemplatePlaceholder.USER_LIST.getPlaceholder(), userList);
                        String message = template.getMessage();
                        message = message.replace(EmailTemplatePlaceholder.LINK_PLACEHOLDER.getPlaceholder(), link);
                        message = message.replace(EmailTemplatePlaceholder.USER_LIST.getPlaceholder(), userList);
                        eventPublisher.publishEvent(EmailEvent.builder()
                            .message(message)
                            .subject(title)
                            .email(email)
                            .build());
                    } else {
                        log.info("Email template with type " + template.getTemplateType() + " is disabled. Email was not sent.");
                    }
                }
            }
        }
    }
}

package dk.digitalidentity.service;

import dk.digitalidentity.Constants;
import dk.digitalidentity.event.EmailEvent;
import dk.digitalidentity.model.entity.EmailTemplate;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.model.entity.enums.EmailTemplateType;
import dk.digitalidentity.model.entity.view.ResponsibleUserView;
import dk.digitalidentity.samlmodule.config.SamlModuleConfiguration;
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
    private final SamlModuleConfiguration diSamlConfiguration;
    private final TaskService taskService;
    private final ApplicationEventPublisher eventPublisher;
    private final ResponsibleUserViewService responsibleUserViewService;
    private final SettingsService settingsService;
    private final EmailTemplateService emailTemplateService;

	@Transactional
	public void notifyTask(final Long taskId) {
		final Task task = taskService.findById(taskId)
				.orElseThrow(() -> new IllegalArgumentException("Task with id: " + taskId + " not found"));

		if (taskService.isTaskDone(task)) {
			return;
		}

		EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.TASK_REMINDER);
		if (!template.isEnabled()) {
			log.info("Email template with type {} is disabled. Email was not sent.", template.getTemplateType());
			return;
		}

		if (task.getResponsibleUsers().isEmpty()) {
			boolean isDbsOversightTask = task.getProperties().stream()
					.anyMatch(p -> Constants.ASSOCIATED_INSPECTION_PROPERTY.equals(p.getKey()));
			if (isDbsOversightTask) {
				String recipientEmail = settingsService.getString(
						Constants.DBS_OVERSIGHT_RECIPIENT_SETTING, "");
				if (!recipientEmail.isEmpty() && !recipientEmail.startsWith("ROLE:")) {
					sendTaskEmail(template, task, recipientEmail, recipientEmail);
				}
			}
			return;
		}

		for (User responsibleUser : task.getResponsibleUsers()) {
			sendTaskEmail(template, task, responsibleUser.getName(), responsibleUser.getEmail());
		}
	}

	public void notifyTaskResponsible(final Task task) {
		if (task.getNotifyResponsible() == null || !task.getNotifyResponsible() || task.getResponsibleUsers().isEmpty()) {
			return;
		}
		EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.TASK_RESPONSIBLE);
		if (!template.isEnabled()) {
			log.info("Email template with type {} is disabled. Email was not sent.", template.getTemplateType());
			return;
		}
		for (User responsibleUser : task.getResponsibleUsers()) {
			if (StringUtils.hasLength(responsibleUser.getEmail())) {
				sendTaskEmail(template, task, responsibleUser.getName(), responsibleUser.getEmail());
			}
		}
	}

	public void notifyOversightByEmail(Task task, String email) {
		EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.TASK_RESPONSIBLE);
		if (!template.isEnabled()) {
			log.info("Email template with type {} is disabled. Email was not sent.", template.getTemplateType());
			return;
		}
		sendTaskEmail(template, task, email, email);
	}

    public void notifyAboutInactiveUsers(Set<String> newlyInactiveUuids) {
        if (!newlyInactiveUuids.isEmpty()) {
            String email = settingsService.getString("inactiveResponsibleEmail", null);
            if (StringUtils.hasLength(email)) {
                List<ResponsibleUserView> responsibleUsers = responsibleUserViewService.findAllIn(newlyInactiveUuids);
                if (!responsibleUsers.isEmpty()) {
                    EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.INACTIVE_USERS);
                    if (template.isEnabled()) {
                        final String baseUrl = diSamlConfiguration.getSp().getBaseUrl();
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
							.templateType(template.getTemplateType())
                            .build());
                    } else {
                        log.info("Email template with type " + template.getTemplateType() + " is disabled. Email was not sent.");
                    }
                }
            }
        }
    }

	private void sendTaskEmail(EmailTemplate template, Task task, String recipientName, String recipientEmail) {
		final String baseUrl = diSamlConfiguration.getSp().getBaseUrl();
		final String url = baseUrl + "/tasks/" + task.getId();
		final String link = "<a href=\"" + url + "\">" + url + "</a>";
		final long days = ChronoUnit.DAYS.between(LocalDate.now(), task.getNextDeadline());

		String title = template.getTitle()
				.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipientName)
				.replace(EmailTemplatePlaceholder.OBJECT_PLACEHOLDER.getPlaceholder(), task.getName())
				.replace(EmailTemplatePlaceholder.LINK_PLACEHOLDER.getPlaceholder(), link)
				.replace(EmailTemplatePlaceholder.DAYS_TILL_DEADLINE.getPlaceholder(), Long.toString(days));

		String message = template.getMessage()
				.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipientName)
				.replace(EmailTemplatePlaceholder.OBJECT_PLACEHOLDER.getPlaceholder(), task.getName())
				.replace(EmailTemplatePlaceholder.LINK_PLACEHOLDER.getPlaceholder(), link)
				.replace(EmailTemplatePlaceholder.DAYS_TILL_DEADLINE.getPlaceholder(), Long.toString(days));

		eventPublisher.publishEvent(EmailEvent.builder()
				.message(message)
				.subject(title)
				.email(recipientEmail)
				.templateType(template.getTemplateType())
				.build());
	}
}

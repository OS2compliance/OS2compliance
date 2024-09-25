package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum EmailTemplateType {
	RISK_REMINDER("Påmindelse om at udfylde risikovurdering", Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.LINK_PLACEHOLDER, EmailTemplatePlaceholder.OBJECT_PLACEHOLDER)),
	TASK_RESPONSIBLE("Mail ved tildeling af ansvar for ny opgave", Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.LINK_PLACEHOLDER, EmailTemplatePlaceholder.OBJECT_PLACEHOLDER)),
	RISK_REPORT("Send risikovurderingsrapport", Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.OBJECT_PLACEHOLDER, EmailTemplatePlaceholder.MESSAGE_FROM_SENDER, EmailTemplatePlaceholder.SENDER)),
	RISK_REPORT_TO_SIGN("Send risikovurderingsrapport til signering", Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.LINK_PLACEHOLDER, EmailTemplatePlaceholder.OBJECT_PLACEHOLDER, EmailTemplatePlaceholder.MESSAGE_FROM_SENDER, EmailTemplatePlaceholder.SENDER)),
	TASK_REMINDER("Påmindelse om en opgaves deadline", Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.LINK_PLACEHOLDER, EmailTemplatePlaceholder.OBJECT_PLACEHOLDER, EmailTemplatePlaceholder.DAYS_TILL_DEADLINE)),
	INACTIVE_USERS("Mail omkring nye inaktive ansvarlige", Arrays.asList(EmailTemplatePlaceholder.LINK_PLACEHOLDER, EmailTemplatePlaceholder.USER_LIST)),
    DPIA_REPORT("Send DPIA-rapport", Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.OBJECT_PLACEHOLDER, EmailTemplatePlaceholder.MESSAGE_FROM_SENDER, EmailTemplatePlaceholder.SENDER)),
    DPIA_REPORT_TO_SIGN("Send DPIA-rapport til signering", Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.LINK_PLACEHOLDER, EmailTemplatePlaceholder.OBJECT_PLACEHOLDER, EmailTemplatePlaceholder.MESSAGE_FROM_SENDER, EmailTemplatePlaceholder.SENDER));


    private final String message;
	private final List<EmailTemplatePlaceholder> emailTemplatePlaceholders;

	EmailTemplateType(String message, List<EmailTemplatePlaceholder> placeholders) {
		this.message = message;
		this.emailTemplatePlaceholders = placeholders;
	}
}

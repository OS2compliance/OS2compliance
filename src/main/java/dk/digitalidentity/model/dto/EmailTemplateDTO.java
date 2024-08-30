package dk.digitalidentity.model.dto;


import dk.digitalidentity.model.entity.enums.EmailTemplatePlaceholder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailTemplateDTO {
	private long id;
	private String title;
	private String message;
	private String notes;
	private String templateTypeText;
	private boolean enabled;
	private List<EmailTemplatePlaceholder> emailTemplatePlaceholders;
}

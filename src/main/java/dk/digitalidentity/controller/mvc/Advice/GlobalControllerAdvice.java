package dk.digitalidentity.controller.mvc.Advice;

import dk.digitalidentity.Constants;
import dk.digitalidentity.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@RequiredArgsConstructor
@ControllerAdvice
public class GlobalControllerAdvice {

	private final SettingsService settingsService;

	@ModelAttribute("allowMultipleTaskResponsible")
	public boolean allowMultipleTaskResponsible() {
		return Boolean.parseBoolean(settingsService.getString(Constants.ALLOW_MULTIPLE_RESPONSIBLE_ON_TASKS, "true"));
	}
}
package dk.digitalidentity.controller.mvc.Advice;

import dk.digitalidentity.Constants;
import dk.digitalidentity.service.SettingsService;
import dk.digitalidentity.service.VersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@RequiredArgsConstructor
@ControllerAdvice
public class GlobalControllerAdvice {

	private final SettingsService settingsService;
	private final VersionService versionService;

	@ModelAttribute("allowMultipleTaskResponsible")
	public boolean allowMultipleTaskResponsible() {
		return Boolean.parseBoolean(settingsService.getString(Constants.ALLOW_MULTIPLE_RESPONSIBLE_ON_TASKS, "true"));
	}

	@ModelAttribute("appVersion")
	public String appVersion() {
		return versionService.getVersion();
	}

	@ModelAttribute("appVersionInfo")
	public String appVersionInfo() {
		return versionService.getVersionInfo();
	}
}

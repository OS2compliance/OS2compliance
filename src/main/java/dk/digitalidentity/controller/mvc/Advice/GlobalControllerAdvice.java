package dk.digitalidentity.controller.mvc.Advice;

import dk.digitalidentity.config.OS2complianceConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@RequiredArgsConstructor
@ControllerAdvice
public class GlobalControllerAdvice {

	private final OS2complianceConfiguration configuration;

	@ModelAttribute("allowMultipleTaskResponsible")
	public boolean allowMultipleTaskResponsible() {
		return configuration.isAllowMultipleTaskResponsibleEnabled();
	}
}
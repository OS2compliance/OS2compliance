package dk.digitalidentity.security;

import dk.digitalidentity.samlmodule.model.TokenUser;
import dk.digitalidentity.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogoutAuditListener {
	private final AuditLogService auditLogService;

	@EventListener
	public void onLogoutSuccess(final LogoutSuccessEvent event) {
		if (event.getAuthentication().getDetails() instanceof TokenUser tokenUser) {
			final String performerUuid = tokenUser.getUsername();
			final String performerName = (String) tokenUser.getAttributes().get(RolePostProcessor.ATTRIBUTE_NAME);
			auditLogService.logLogout(performerUuid, performerName);
		}
	}
}

package dk.digitalidentity.config;

import dk.digitalidentity.service.AuditLogService;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditEntityChangeListenerConfig {
	private final EntityManagerFactory entityManagerFactory;
	private final AuditLogService auditLogService;

	@PostConstruct
	public void registerListeners() {
		final SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
		final EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
		final AuditEntityChangeListener listener = new AuditEntityChangeListener(auditLogService);
		registry.appendListeners(EventType.POST_COMMIT_INSERT, listener);
		registry.appendListeners(EventType.POST_COMMIT_UPDATE, listener);
		registry.appendListeners(EventType.POST_COMMIT_DELETE, listener);
	}
}

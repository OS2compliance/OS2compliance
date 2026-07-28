package dk.digitalidentity.config;

import dk.digitalidentity.samlmodule.model.TokenUser;
import dk.digitalidentity.security.RolePostProcessor;
import dk.digitalidentity.service.AuditLogService;
import dk.digitalidentity.service.AuditedEntityRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes one {@link dk.digitalidentity.model.entity.AuditLog} row per @Audited entity, per
 * transaction, so the admin auditlog grid shows a single, human-meaningful event ("created",
 * "updated", "deleted") instead of every individual SQL statement Hibernate happens to issue
 * (e.g. an insert immediately followed by an update as default related objects are attached).
 *
 * Registers itself as a raw Hibernate event listener (Hibernate listeners aren't Spring beans and
 * can't be wired declaratively), while still being a normal Spring @Component so it can be
 * constructed with its dependencies and can hook Spring's transaction synchronization.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEntityChangeListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

	private final AuditLogService auditLogService;
	private final EntityManagerFactory entityManagerFactory;

	private record PendingChange(String performerUuid, String performerName, String entityType, String entityId, String entityName, AuditLogService.ChangeType changeType) {
	}

	private final ThreadLocal<Map<String, PendingChange>> pendingChanges = ThreadLocal.withInitial(LinkedHashMap::new);
	private final ThreadLocal<Boolean> synchronizationRegistered = ThreadLocal.withInitial(() -> false);

	@PostConstruct
	public void register() {
		final SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
		final EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
		registry.appendListeners(EventType.POST_INSERT, this);
		registry.appendListeners(EventType.POST_UPDATE, this);
		registry.appendListeners(EventType.POST_DELETE, this);
	}

	@Override
	public void onPostInsert(final PostInsertEvent event) {
		accumulate(event.getEntity(), event.getId(), AuditLogService.ChangeType.CREATE);
	}

	@Override
	public void onPostUpdate(final PostUpdateEvent event) {
		accumulate(event.getEntity(), event.getId(), AuditLogService.ChangeType.UPDATE);
	}

	@Override
	public void onPostDelete(final PostDeleteEvent event) {
		accumulate(event.getEntity(), event.getId(), AuditLogService.ChangeType.DELETE);
	}

	@Override
	public boolean requiresPostCommitHandling(final EntityPersister persister) {
		return false;
	}

	/**
	 * Buffers the change on this thread instead of writing it immediately, so that several writes
	 * to the same entity within one transaction (e.g. create-then-attach-defaults) collapse into a
	 * single auditlog row, flushed only once the transaction actually commits.
	 */
	private void accumulate(final Object entity, final Object id, final AuditLogService.ChangeType changeType) {
		if (!AuditedEntityRegistry.isAudited(entity.getClass())) {
			return;
		}

		final String entityType = entity.getClass().getSimpleName();
		final String entityId = id != null ? id.toString() : null;
		final String key = entityType + ":" + entityId;

		final Map<String, PendingChange> changes = pendingChanges.get();
		final PendingChange existing = changes.get(key);
		if (existing != null && existing.changeType() == AuditLogService.ChangeType.CREATE && changeType == AuditLogService.ChangeType.DELETE) {
			// created and deleted again within the same transaction - nothing meaningful happened
			changes.remove(key);
			return;
		}

		final AuditLogService.ChangeType effectiveType = existing != null && existing.changeType() == AuditLogService.ChangeType.CREATE
				? AuditLogService.ChangeType.CREATE
				: changeType;

		changes.put(key, new PendingChange(currentPerformerUuid(), currentPerformerName(), entityType, entityId, AuditedEntityRegistry.extractName(entity), effectiveType));

		registerFlushOnCommit();
	}

	private void registerFlushOnCommit() {
		if (synchronizationRegistered.get() || !TransactionSynchronizationManager.isSynchronizationActive()) {
			return;
		}
		synchronizationRegistered.set(true);

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				for (final PendingChange change : pendingChanges.get().values()) {
					try {
						auditLogService.logEntityChange(
								change.performerUuid(), change.performerName(),
								change.entityType(), change.entityId(), change.entityName(),
								change.changeType()
						);
					} catch (final Exception e) {
						// afterCommit() exceptions are swallowed by Spring's transaction manager (only logged),
						// so without this the whole auditlog write for this change disappears silently.
						log.error("Failed to write auditlog row for {}:{} ({})", change.entityType(), change.entityId(), change.changeType(), e);
					}
				}
			}

			@Override
			public void afterCompletion(final int status) {
				pendingChanges.remove();
				synchronizationRegistered.remove();
			}
		});
	}

	private String currentPerformerUuid() {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getDetails() instanceof TokenUser tokenUser) {
			return tokenUser.getUsername();
		}
		return "system";
	}

	private String currentPerformerName() {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getDetails() instanceof TokenUser tokenUser) {
			final Object name = tokenUser.getAttributes() != null ? tokenUser.getAttributes().get(RolePostProcessor.ATTRIBUTE_NAME) : null;
			return name != null ? name.toString() : tokenUser.getUsername();
		}
		return "System";
	}
}

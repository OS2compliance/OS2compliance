package dk.digitalidentity.config;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.EmailTemplate;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.StandardTemplate;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.samlmodule.model.TokenUser;
import dk.digitalidentity.security.RolePostProcessor;
import dk.digitalidentity.service.AuditLogService;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

/**
 * Writes an {@link dk.digitalidentity.model.entity.AuditLog} row for every create/update/delete
 * of an @Audited entity, so each row in the admin auditlog grid can link to its Envers history.
 */
public class AuditEntityChangeListener implements PostCommitInsertEventListener, PostCommitUpdateEventListener, PostCommitDeleteEventListener {

	private static final Set<Class<?>> AUDITED_ENTITY_CLASSES = Set.of(
			Asset.class, ThreatAssessment.class, DPIA.class, Incident.class, Task.class,
			Register.class, Document.class, StandardSection.class, StandardTemplate.class,
			StandardTemplateSection.class, Supplier.class, User.class, ChoiceList.class, EmailTemplate.class
	);

	private final AuditLogService auditLogService;

	public AuditEntityChangeListener(final AuditLogService auditLogService) {
		this.auditLogService = auditLogService;
	}

	@Override
	public void onPostInsert(final PostInsertEvent event) {
		log(event.getEntity(), event.getId(), AuditLogService.ChangeType.CREATE);
	}

	@Override
	public void onPostInsertCommitFailed(final PostInsertEvent event) {
	}

	@Override
	public void onPostUpdate(final PostUpdateEvent event) {
		log(event.getEntity(), event.getId(), AuditLogService.ChangeType.UPDATE);
	}

	@Override
	public void onPostUpdateCommitFailed(final PostUpdateEvent event) {
	}

	@Override
	public void onPostDelete(final PostDeleteEvent event) {
		log(event.getEntity(), event.getId(), AuditLogService.ChangeType.DELETE);
	}

	@Override
	public void onPostDeleteCommitFailed(final PostDeleteEvent event) {
	}

	@Override
	public boolean requiresPostCommitHandling(final EntityPersister persister) {
		return AUDITED_ENTITY_CLASSES.contains(persister.getMappedClass());
	}

	private void log(final Object entity, final Object id, final AuditLogService.ChangeType changeType) {
		if (!AUDITED_ENTITY_CLASSES.contains(entity.getClass())) {
			return;
		}

		auditLogService.logEntityChange(
				currentPerformerUuid(),
				currentPerformerName(),
				entity.getClass().getSimpleName(),
				id != null ? id.toString() : null,
				extractEntityName(entity),
				changeType
		);
	}

	private String extractEntityName(final Object entity) {
		if (entity instanceof Relatable relatable) {
			return relatable.getName();
		}
		if (entity instanceof User user) {
			return user.getName();
		}
		if (entity instanceof StandardTemplate standardTemplate) {
			return standardTemplate.getName();
		}
		if (entity instanceof StandardTemplateSection standardTemplateSection) {
			return standardTemplateSection.getSection();
		}
		if (entity instanceof ChoiceList choiceList) {
			return choiceList.getName();
		}
		if (entity instanceof EmailTemplate emailTemplate) {
			return emailTemplate.getTitle();
		}
		return null;
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
			final Object name = tokenUser.getAttributes().get(RolePostProcessor.ATTRIBUTE_NAME);
			return name != null ? name.toString() : tokenUser.getUsername();
		}
		return "System";
	}
}

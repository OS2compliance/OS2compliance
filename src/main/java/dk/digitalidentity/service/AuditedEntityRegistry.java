package dk.digitalidentity.service;

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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for which entities are @Audited via Hibernate Envers, and how to get a
 * human-readable name / a typed id for them. Used by both {@link dk.digitalidentity.config.AuditEntityChangeListener}
 * (writing auditlog rows) and {@link EnversHistoryService} (reading revision history for those rows).
 */
public final class AuditedEntityRegistry {

	private static final Map<String, Class<?>> CLASSES_BY_NAME = Map.ofEntries(
			Map.entry(Asset.class.getSimpleName(), Asset.class),
			Map.entry(ThreatAssessment.class.getSimpleName(), ThreatAssessment.class),
			Map.entry(DPIA.class.getSimpleName(), DPIA.class),
			Map.entry(Incident.class.getSimpleName(), Incident.class),
			Map.entry(Task.class.getSimpleName(), Task.class),
			Map.entry(Register.class.getSimpleName(), Register.class),
			Map.entry(Document.class.getSimpleName(), Document.class),
			Map.entry(StandardSection.class.getSimpleName(), StandardSection.class),
			Map.entry(StandardTemplate.class.getSimpleName(), StandardTemplate.class),
			Map.entry(StandardTemplateSection.class.getSimpleName(), StandardTemplateSection.class),
			Map.entry(Supplier.class.getSimpleName(), Supplier.class),
			Map.entry(User.class.getSimpleName(), User.class),
			Map.entry(ChoiceList.class.getSimpleName(), ChoiceList.class),
			Map.entry(EmailTemplate.class.getSimpleName(), EmailTemplate.class)
	);

	private static final Set<String> STRING_KEYED_ENTITIES = Set.of(
			User.class.getSimpleName(), StandardTemplate.class.getSimpleName(), StandardTemplateSection.class.getSimpleName()
	);

	private AuditedEntityRegistry() {
	}

	public static boolean isAudited(final Class<?> entityClass) {
		return CLASSES_BY_NAME.containsKey(entityClass.getSimpleName());
	}

	public static Class<?> resolveClass(final String entityType) {
		return CLASSES_BY_NAME.get(entityType);
	}

	public static Collection<Class<?>> allAuditedClasses() {
		return CLASSES_BY_NAME.values();
	}

	/**
	 * Envers' AuditReader needs the id typed as it's actually declared on the entity (String vs Long),
	 * but auditlog rows only ever carry the id as a String, so this converts back.
	 */
	public static Object resolveId(final String entityType, final String entityId) {
		return STRING_KEYED_ENTITIES.contains(entityType) ? entityId : Long.valueOf(entityId);
	}

	public static String extractName(final Object entity) {
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
}

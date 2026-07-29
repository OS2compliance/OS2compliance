package dk.digitalidentity.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnversHistoryService {
	private final EntityManager entityManager;
	private final MessageSource messageSource;

	public record FieldDiff(String field, String oldValue, String newValue) {
	}

	/**
	 * Diffs the entity's state as of the given revision against its state as of the revision
	 * immediately preceding it *for that entity* - revision numbers are a global sequence shared
	 * by every audited entity, so "revision - 1" would not generally be the entity's own previous
	 * revision.
	 */
	public List<FieldDiff> getDiffForRevision(final String entityType, final String entityId, final Integer revision) {
		final Class<?> entityClass = AuditedEntityRegistry.resolveClass(entityType);
		if (entityClass == null || revision == null) {
			return List.of();
		}

		final Object id = AuditedEntityRegistry.resolveId(entityType, entityId);
		final AuditReader auditReader = AuditReaderFactory.get(entityManager);
		final List<Number> revisions = auditReader.getRevisions(entityClass, id);
		final int index = revisions.indexOf(revision);
		if (index < 0) {
			return List.of();
		}
		if (index == 0) {
			// this was the entity's creation - nothing to diff against
			return List.of();
		}

		final Number previousRevision = revisions.get(index - 1);
		final Object previous = auditReader.find(entityClass, id, previousRevision);
		final Object latest = auditReader.find(entityClass, id, revision);
		if (latest == null) {
			// this revision is the entity's deletion - Envers has no state to diff at that point
			return List.of();
		}
		return diff(entityClass, previous, latest);
	}

	private List<FieldDiff> diff(final Class<?> entityClass, final Object previous, final Object latest) {
		final List<FieldDiff> result = new ArrayList<>();
		final Set<String> mappedColumnNames = basicAttributeNames(entityClass);
		try {
			final BeanInfo beanInfo = Introspector.getBeanInfo(latest.getClass(), Object.class);
			for (final PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
				if (!mappedColumnNames.contains(descriptor.getName())) {
					// not an actual @Column, e.g. a computed helper getter like getLocalizedEnumValues()
					continue;
				}
				final Method getter = descriptor.getReadMethod();
				if (getter == null) {
					continue;
				}
				final Object oldValue = previous != null ? getter.invoke(previous) : null;
				final Object newValue = getter.invoke(latest);
				if (!Objects.equals(normalize(oldValue), normalize(newValue))) {
					result.add(new FieldDiff(translateField(descriptor.getName()),
							oldValue != null ? oldValue.toString() : null,
							newValue != null ? newValue.toString() : null));
				}
			}
		} catch (final Exception e) {
			throw new IllegalStateException("Kunne ikke sammenligne revisioner", e);
		}
		return result;
	}

	/**
	 * Only the entity's actual @Column-mapped, non-relational attributes - excludes computed
	 * helper getters (e.g. getLocalizedEnumValues(), getManagerUuids()) that Introspector would
	 * otherwise pick up just because they look like bean properties.
	 */
	private Set<String> basicAttributeNames(final Class<?> entityClass) {
		return entityManager.getMetamodel().entity(entityClass).getAttributes().stream()
				.filter(attribute -> attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC)
				.map(Attribute::getName)
				.collect(Collectors.toSet());
	}

	/**
	 * Treats a blank string the same as null, so a field that was empty and got clicked into but
	 * left untouched doesn't show up as a "changed" (nothing-to-nothing) diff row.
	 */
	private Object normalize(final Object value) {
		if (value instanceof String s && s.isBlank()) {
			return null;
		}
		return value;
	}

	private String translateField(final String propertyName) {
		try {
			return messageSource.getMessage("auditlog.field." + propertyName, null, Locale.of("da"));
		} catch (final NoSuchMessageException e) {
			return humanize(propertyName);
		}
	}

	/**
	 * Fallback for any property without a messages.properties entry: turns e.g. "someNewField" into "Some new field".
	 */
	private String humanize(final String propertyName) {
		final String spaced = propertyName.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
		return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
	}
}

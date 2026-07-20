package dk.digitalidentity.service;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.EmailTemplate;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.StandardTemplate;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.stereotype.Service;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EnversHistoryService {
	private final EntityManager entityManager;

	private static final Map<String, Class<?>> AUDITED_ENTITY_CLASSES_BY_NAME = Map.ofEntries(
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

	private static final Set<Class<?>> DIFFABLE_PROPERTY_TYPES = Set.of(
			String.class, Boolean.class, boolean.class, Integer.class, int.class, Long.class, long.class,
			Double.class, double.class, BigDecimal.class, LocalDate.class, LocalDateTime.class
	);

	public record FieldDiff(String field, String oldValue, String newValue) {
	}

	public List<FieldDiff> getLatestDiff(final String entityType, final String entityId) {
		final Class<?> entityClass = AUDITED_ENTITY_CLASSES_BY_NAME.get(entityType);
		if (entityClass == null) {
			return List.of();
		}

		final Object id = resolveId(entityClass, entityId);
		final AuditReader auditReader = AuditReaderFactory.get(entityManager);
		final List<Number> revisions = auditReader.getRevisions(entityClass, id);
		if (revisions.size() < 2) {
			return List.of();
		}

		final Number previousRevision = revisions.get(revisions.size() - 2);
		final Number latestRevision = revisions.get(revisions.size() - 1);
		final Object previous = auditReader.find(entityClass, id, previousRevision);
		final Object latest = auditReader.find(entityClass, id, latestRevision);
		return diff(previous, latest);
	}

	private Object resolveId(final Class<?> entityClass, final String entityId) {
		return switch (entityClass.getSimpleName()) {
			case "User", "StandardTemplate", "StandardTemplateSection" -> entityId;
			default -> Long.valueOf(entityId);
		};
	}

	private List<FieldDiff> diff(final Object previous, final Object latest) {
		final List<FieldDiff> result = new ArrayList<>();
		try {
			final BeanInfo beanInfo = Introspector.getBeanInfo(latest.getClass(), Object.class);
			for (final PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
				final Method getter = descriptor.getReadMethod();
				if (getter == null || !DIFFABLE_PROPERTY_TYPES.contains(getter.getReturnType())) {
					continue;
				}
				final Object oldValue = getter.invoke(previous);
				final Object newValue = getter.invoke(latest);
				if (!Objects.equals(oldValue, newValue)) {
					result.add(new FieldDiff(descriptor.getName(),
							oldValue != null ? oldValue.toString() : null,
							newValue != null ? newValue.toString() : null));
				}
			}
		} catch (final Exception e) {
			throw new IllegalStateException("Kunne ikke sammenligne revisioner", e);
		}
		return result;
	}
}

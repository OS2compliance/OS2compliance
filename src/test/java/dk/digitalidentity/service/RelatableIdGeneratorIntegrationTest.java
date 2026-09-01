package dk.digitalidentity.service;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.model.entity.Relatable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the two rules {@link Relatable#ID_GENERATOR}'s Javadoc depends on: a subclass must never
 * declare its own id mapping, and its table must never carry a leftover {@code AUTO_INCREMENT} on
 * {@code id} - either hands that subclass a segment of its own, colliding with the one shared
 * generator every {@link Relatable} draws from. MR !542 fixed the two known offenders (dpia /
 * dbs_asset, then contacts / suppliers); both assertions below are green today, and exist to catch
 * the next one - including a brand new subclass, since the class list is discovered, not hardcoded.
 */
public class RelatableIdGeneratorIntegrationTest extends BaseIntegrationTest {

	private static final List<Class<? extends Annotation>> OWN_ID_ANNOTATIONS =
			List.of(Id.class, GeneratedValue.class, TableGenerator.class, SequenceGenerator.class);

	@Autowired
	private EntityManager entityManager;

	static Stream<Class<?>> relatableSubclasses() {
		final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AssignableTypeFilter(Relatable.class));

		return scanner.findCandidateComponents("dk.digitalidentity.model.entity").stream()
				.<Class<?>>map(bean -> {
					try {
						return Class.forName(bean.getBeanClassName());
					} catch (final ClassNotFoundException e) {
						throw new IllegalStateException(e);
					}
				})
				.filter(clazz -> clazz != Relatable.class);
	}

	@ParameterizedTest
	@MethodSource("relatableSubclasses")
	void subclassDoesNotDeclareItsOwnIdMapping(final Class<?> subclass) {
		final List<String> violations = new ArrayList<>();
		collectViolations(subclass.getDeclaredFields(), violations);
		collectViolations(subclass.getDeclaredMethods(), violations);

		assertThat(violations)
				.as("%s declares its own id mapping - it must keep drawing from Relatable.ID_GENERATOR instead, "
						+ "or Hibernate gives it a segment of its own and it collides with the shared one", subclass.getSimpleName())
				.isEmpty();
	}

	@ParameterizedTest
	@MethodSource("relatableSubclasses")
	void tableHasNoLeftoverAutoIncrementOnId(final Class<?> subclass) {
		final String table = subclass.getAnnotation(Table.class).name();

		assertThat(idColumnExtra(table))
				.as("%s.id still has AUTO_INCREMENT - ids must come only from hibernate_sequences (V1_125 dropped this for contacts/suppliers)", table)
				.doesNotContainIgnoringCase("auto_increment");
	}

	private static void collectViolations(final AccessibleObject[] members, final List<String> violations) {
		for (final AccessibleObject member : members) {
			for (final Class<? extends Annotation> annotationType : OWN_ID_ANNOTATIONS) {
				if (member.isAnnotationPresent(annotationType)) {
					violations.add(((Member) member).getName() + " has @" + annotationType.getSimpleName());
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private String idColumnExtra(final String table) {
		final List<String> extra = entityManager.createNativeQuery(
						"SELECT LOWER(extra) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = :table AND column_name = 'id'")
				.setParameter("table", table)
				.getResultList();
		assertThat(extra).as("table %s has no id column", table).isNotEmpty();
		return extra.get(0);
	}
}

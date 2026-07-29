package dk.digitalidentity.service;

import dk.digitalidentity.BaseIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.type.Type;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against the *_aud tables (manually maintained DDL) drifting away from the entities they
 * audit: if a column is added to an @Audited entity's table without a matching column on its
 * _aud table, Envers only fails at write time in production - nothing in the build catches it.
 */
public class AuditedEntitySchemaIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private EntityManager entityManager;

	static Stream<Class<?>> auditedClasses() {
		return AuditedEntityRegistry.allAuditedClasses().stream();
	}

	@ParameterizedTest
	@MethodSource("auditedClasses")
	void auditTableHasEveryColumnMappedByItsEntity(final Class<?> entityClass) {
		final String tableName = entityClass.getAnnotation(Table.class).name();
		final String auditTableName = tableName + "_aud";

		final Set<String> mappedColumns = mappedColumnsOf(entityClass);
		final Set<String> auditColumns = columnsOf(auditTableName);

		assertThat(auditColumns)
				.as("%s is missing columns mapped by %s - update V1_118__add_envers_audit_tables.sql", auditTableName, entityClass.getSimpleName())
				.containsAll(mappedColumns);
	}

	/**
	 * The actual DB columns Hibernate uses for this entity's own basic (non-relational, non-collection)
	 * properties - what Envers needs a matching column for on the _aud table. Deliberately not compared
	 * against the full raw table schema, since a table can carry legacy columns no longer mapped by JPA.
	 */
	private Set<String> mappedColumnsOf(final Class<?> entityClass) {
		final SessionFactoryImplementor sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactoryImplementor.class);
		final AbstractEntityPersister persister = (AbstractEntityPersister) sessionFactory.getMappingMetamodel().getEntityDescriptor(entityClass.getName());

		final Set<String> columns = new HashSet<>();
		for (final String column : persister.getIdentifierColumnNames()) {
			columns.add(column.toLowerCase());
		}
		for (final String propertyName : persister.getPropertyNames()) {
			final Type type = persister.getPropertyType(propertyName);
			if (type.isAssociationType() || type.isCollectionType()) {
				continue;
			}
			for (final String column : persister.getPropertyColumnNames(propertyName)) {
				// null for properties backed by a @Formula rather than an actual column
				if (column != null) {
					columns.add(column.toLowerCase());
				}
			}
		}
		return columns;
	}

	@SuppressWarnings("unchecked")
	private Set<String> columnsOf(final String table) {
		final List<String> columns = entityManager.createNativeQuery(
						"SELECT LOWER(column_name) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = :table")
				.setParameter("table", table)
				.getResultList();
		assertThat(columns).as("table %s does not exist", table).isNotEmpty();
		return new HashSet<>(columns);
	}
}

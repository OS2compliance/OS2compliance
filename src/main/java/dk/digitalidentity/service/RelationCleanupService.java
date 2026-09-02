package dk.digitalidentity.service;

import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.Contact;
import dk.digitalidentity.model.entity.DBSAsset;
import dk.digitalidentity.model.entity.DBSOversight;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.Precaution;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.TaskLog;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.enums.RelationType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class RelationCleanupService {
	private final RelationDao relationDao;
	private final JdbcTemplate jdbcTemplate;

	/**
	 * The tables backing every {@link dk.digitalidentity.model.entity.Relatable} subclass - the ones
	 * that draw ids from {@code shared_id_generator} and must therefore never contain the same id twice.
	 */
	private static final List<String> RELATABLE_TABLES = List.of(
			"assets", "contacts", "dbs_asset", "documents", "dpia", "incidents",
			"precautions", "registers", "standard_sections", "suppliers", "tasks",
			"task_logs", "threat_assessments", "threat_assessment_responses"
	);

	/**
	 * {@code custom_threats} draws from the same generator (see
	 * {@link dk.digitalidentity.model.entity.CustomThreat}) without being a Relatable subclass, so it
	 * counts towards how much headroom the generator has left but is not itself scanned for duplicates.
	 */
	private static final List<String> ID_GENERATOR_TABLES =
			Stream.concat(RELATABLE_TABLES.stream(), Stream.of("custom_threats")).toList();

	@Transactional(readOnly = true)
	public Map<Class<?>, Set<Relation>> findBrokenRelations() {
		Map<RelationType, Class<?>> classPerType = new EnumMap<>(RelationType.class);
		classPerType.put(RelationType.SUPPLIER, Supplier.class);
		classPerType.put(RelationType.CONTACT, Contact.class);
		classPerType.put(RelationType.TASK, Task.class);
		classPerType.put(RelationType.DOCUMENT, Document.class);
		classPerType.put(RelationType.TASK_LOG, TaskLog.class);
		classPerType.put(RelationType.REGISTER, Register.class);
		classPerType.put(RelationType.ASSET, Asset.class);
		classPerType.put(RelationType.STANDARD_SECTION, StandardSection.class);
		classPerType.put(RelationType.THREAT_ASSESSMENT, ThreatAssessment.class);
		classPerType.put(RelationType.THREAT_ASSESSMENT_RESPONSE, ThreatAssessmentResponse.class);
		classPerType.put(RelationType.PRECAUTION, Precaution.class);
		classPerType.put(RelationType.DBSASSET, DBSAsset.class);
		classPerType.put(RelationType.DBSOVERSIGHT, DBSOversight.class);
		classPerType.put(RelationType.INCIDENT, Incident.class);
		classPerType.put(RelationType.DPIA, DPIA.class);

		Map<Class<?>, Set<Relation>> brokenRelations = new HashMap<>();
		for (Map.Entry<RelationType, Class<?>> entry : classPerType.entrySet()) {
			brokenRelations.put(entry.getValue(), new HashSet<>(relationDao.findAll(hasBrokenRelations(entry.getKey(), entry.getValue()))));
		}

		return brokenRelations;
	}

	/**
	 * Every id shared by two or more Relatable tables, found directly against the tables themselves.
	 * The previous approach only ever saw ids referenced from the {@code relations} join table, so a
	 * collision between two rows that happen not to be related to anything went undetected.
	 */
	@Transactional(readOnly = true)
	public Map<Long, List<String>> findAllDuplicateIds() {
		final String unionSelect = RELATABLE_TABLES.stream()
				.map(table -> "SELECT id, '%s' AS table_name FROM %s".formatted(table, table))
				.collect(Collectors.joining(" UNION ALL "));

		final List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
				SELECT id, GROUP_CONCAT(table_name ORDER BY table_name) AS tables
				FROM (%s) all_relatables
				GROUP BY id
				HAVING COUNT(*) > 1
				""".formatted(unionSelect));

		final Map<Long, List<String>> duplicates = new LinkedHashMap<>();
		for (final Map<String, Object> row : rows) {
			duplicates.put(((Number) row.get("id")).longValue(), List.of(((String) row.get("tables")).split(",")));
		}
		return duplicates;
	}

	/**
	 * Whether the next block the generator hands out could collide with an id already in use.
	 * {@code allocationSize = 50}'s PooledOptimizer serves {@code [next_val - 48 .. next_val + 1]}
	 * before it touches {@code next_val} again (see {@code Relatable.ID_GENERATOR}'s Javadoc), so
	 * {@code next_val} must clear the highest id in use by more than that.
	 */
	@Transactional(readOnly = true)
	public GeneratorHeadroom checkGeneratorHeadroom() {
		final Long nextVal = jdbcTemplate.queryForObject(
				"SELECT next_val FROM hibernate_sequences WHERE sequence_name = 'default'", Long.class);

		final String unionSelect = ID_GENERATOR_TABLES.stream()
				.map(table -> "SELECT id FROM " + table)
				.collect(Collectors.joining(" UNION ALL "));
		final Long highestId = jdbcTemplate.queryForObject(
				"SELECT MAX(id) FROM (%s) all_ids".formatted(unionSelect), Long.class);

		return new GeneratorHeadroom(nextVal == null ? 0L : nextVal, highestId == null ? 0L : highestId);
	}

	public record GeneratorHeadroom(long nextVal, long highestIdInUse) {
		public boolean isAtRisk() {
			return nextVal - 48 <= highestIdInUse;
		}
	}

	private static Specification<Relation> hasBrokenRelation(
			RelationType type,
			Class<?> entityClass,
			String typeField,
			String idField) {
		return (root, query, cb) -> {
			Subquery<Long> subquery = Objects.requireNonNull(query, "query must not be null")
					.subquery(Long.class);
			Root<?> entity = subquery.from(entityClass);

			subquery.select(cb.literal(1L))
					.where(cb.equal(entity.get("id"), root.get(idField)));

			return cb.and(
					cb.equal(root.get(typeField), type),
					cb.not(cb.exists(subquery))
			);
		};
	}

	public static Specification<Relation> hasBrokenRelationA(RelationType type, Class<?> entityClass) {
		return hasBrokenRelation(type, entityClass, "relationAType", "relationAId");
	}

	public static Specification<Relation> hasBrokenRelationB(RelationType type, Class<?> entityClass) {
		return hasBrokenRelation(type, entityClass, "relationBType", "relationBId");
	}

	public static Specification<Relation> hasBrokenRelations(RelationType type, Class<?> entityClass) {
		return hasBrokenRelationA(type, entityClass).or(hasBrokenRelationB(type, entityClass));
	}
}

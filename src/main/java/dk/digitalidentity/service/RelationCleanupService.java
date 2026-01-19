package dk.digitalidentity.service;

import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.dao.ContactDao;
import dk.digitalidentity.dao.DBSAssetDao;
import dk.digitalidentity.dao.DBSOversightDao;
import dk.digitalidentity.dao.DPIADao;
import dk.digitalidentity.dao.DocumentDao;
import dk.digitalidentity.dao.IncidentDao;
import dk.digitalidentity.dao.PrecautionDao;
import dk.digitalidentity.dao.RegisterDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.dao.StandardSectionDao;
import dk.digitalidentity.dao.SupplierDao;
import dk.digitalidentity.dao.TaskDao;
import dk.digitalidentity.dao.TaskLogDao;
import dk.digitalidentity.dao.ThreatAssessmentDao;
import dk.digitalidentity.dao.ThreatAssessmentResponseDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.Contact;
import dk.digitalidentity.model.entity.DBSAsset;
import dk.digitalidentity.model.entity.DBSOversight;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.Precaution;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RelationCleanupService {
	private final RelationDao relationDao;
	private final SupplierDao supplierDao;
	private final ContactDao contactDao;
	private final TaskDao taskDao;
	private final DocumentDao documentDao;
	private final TaskLogDao taskLogDao;
	private final RegisterDao registerDao;
	private final AssetDao assetDao;
	private final StandardSectionDao standardSectionDao;
	private final ThreatAssessmentDao threatAssessmentDao;
	private final ThreatAssessmentResponseDao threatAssessmentResponseDao;
	private final PrecautionDao precautionDao;
	private final DBSAssetDao dBSAssetDao;
	private final DBSOversightDao dBSOversightDao;
	private final IncidentDao incidentDao;
	private final DPIADao dPIADao;

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

	@Transactional(readOnly = true)
	public Map<RelationType, Collection<? extends Relatable>> findAllDuplicateIds() {
		Map<RelationType, Collection<? extends Relatable>> duplicateIds = new EnumMap<>(RelationType.class);

		// map dao functions for relationtypes
		@FunctionalInterface
		interface EntityDaoFunction {
			Collection<? extends Relatable> apply(List<Long> ids);
		}

		Map<RelationType, EntityDaoFunction> queryFunctionPerType = new EnumMap<>(RelationType.class);
		queryFunctionPerType.put(RelationType.SUPPLIER, supplierDao::findAllById);
		queryFunctionPerType.put(RelationType.CONTACT, contactDao::findAllById);
		queryFunctionPerType.put(RelationType.TASK, taskDao::findAllById);
		queryFunctionPerType.put(RelationType.DOCUMENT, documentDao::findAllById);
		queryFunctionPerType.put(RelationType.TASK_LOG, taskLogDao::findAllById);
		queryFunctionPerType.put(RelationType.REGISTER, registerDao::findAllById);
		queryFunctionPerType.put(RelationType.ASSET, assetDao::findAllById);
		queryFunctionPerType.put(RelationType.STANDARD_SECTION, standardSectionDao::findAllById);
		queryFunctionPerType.put(RelationType.THREAT_ASSESSMENT, threatAssessmentDao::findAllById);
		queryFunctionPerType.put(RelationType.THREAT_ASSESSMENT_RESPONSE, threatAssessmentResponseDao::findAllById);
		queryFunctionPerType.put(RelationType.PRECAUTION, precautionDao::findAllById);
		queryFunctionPerType.put(RelationType.DBSASSET, dBSAssetDao::findAllById);
		queryFunctionPerType.put(RelationType.INCIDENT, incidentDao::findAllById);
		queryFunctionPerType.put(RelationType.DPIA, dPIADao::findAllById);

		// find all Relation ids and map them
		List<Relation> relations = relationDao.findAll();
		Map<RelationType, Set<Long>> idsByType = new EnumMap<>(RelationType.class);
		for (Relation relation : relations) {
			RelationType relationTypeA = relation.getRelationAType();
			Long relationAId = relation.getRelationAId();
			idsByType.computeIfAbsent(relationTypeA, k -> new HashSet<>())
					.add(relationAId);

			RelationType relationTypeB = relation.getRelationBType();
			Long relationBId = relation.getRelationBId();
			idsByType.computeIfAbsent(relationTypeB, k -> new HashSet<>())
					.add(relationBId);

		}
		// for each type, find the united set of every OTHER relationtypes ids
		for (RelationType relationType : idsByType.keySet()) {
			Set<Long> allOtherIds = idsByType.entrySet().stream()
					.filter(entry -> !entry.getKey().equals(relationType))
					.flatMap(entry -> entry.getValue().stream())
					.collect(Collectors.toSet());

			// search this relatables table for any of the other Ids
			Collection<? extends Relatable> relatablesWithDuplicateId = queryFunctionPerType
					.get(relationType)
					.apply(allOtherIds.stream().toList());

			duplicateIds.put(relationType, relatablesWithDuplicateId);
		}

		return duplicateIds;
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

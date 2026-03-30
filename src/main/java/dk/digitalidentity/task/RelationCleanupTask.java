package dk.digitalidentity.task;

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
import dk.digitalidentity.service.RelationCleanupService;
import dk.digitalidentity.service.RelationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class RelationCleanupTask {
	private final RelationService relationService;
	private final RelationCleanupService relationCleanupService;

	@Transactional
	@Scheduled(cron = "${os2complicance.task.relation.cleanup.cron}")
	public void cleanupRelations() {
		final Map<String, RelationType> CLASS_NAME_TO_TYPE = createClassNameToTypeMap();

		// find all relations that points at an incorrect id
		Map<Class<?>, Set<Relation>> brokenRelations = relationCleanupService.findBrokenRelations();

		if (brokenRelations.isEmpty()) {
			return;
		}

		// log warnings for found broken relations
		for (Map.Entry<Class<?>, Set<Relation>> entry : brokenRelations.entrySet()) {
			if (entry.getValue().isEmpty()) {
				continue;
			}

			String type = entry.getKey().getSimpleName();
			int count = entry.getValue().size();

			log.warn("Found {} broken relations of type {}:", count, type);
			entry.getValue().forEach(relation -> log.warn(formatBrokenRelation(relation, CLASS_NAME_TO_TYPE.get(entry.getKey().getSimpleName()))));
		}

		// stream to set to eliminate duplicates (those where both sides of relation is broken)
		Set<Long> toDelete = brokenRelations.values().stream().flatMap(Set::stream).map(Relation::getId).collect(Collectors.toSet());
		log.warn("Found a total of unique {} broken relations in Relations table, that will be deleted", toDelete.size());

		// delete broken relations
		relationService.deleteAllByIds(toDelete);
	}

	@Transactional
	@Scheduled(cron = "${os2complicance.task.relation.duplicate.cron}")
	public void findDuplicateRelationIds() {
		Map<RelationType, Collection<? extends Relatable>> duplicateIDRelatable = relationCleanupService.findAllDuplicateIds();

		log.info("Searching for Relatables with ids among other relations");
		int foundIssuesCount = 0;
		for (Map.Entry<RelationType, Collection<? extends Relatable>> entry : duplicateIDRelatable.entrySet()) {
			if (entry.getValue().isEmpty()) {
				continue;
			}
			log.warn("Type: {}, Ids: {}",
					entry.getKey(),
					entry.getValue().stream()
							.map(Relatable::getId)
							.sorted()
							.toList());
			foundIssuesCount += entry.getValue().size();
		}
		if (foundIssuesCount > 0) {
			log.warn("Found {} relatables with ids found among other relations:", foundIssuesCount);
		}
	}

	private static Map<String, RelationType> createClassNameToTypeMap() {
		Map<String, RelationType> map = new HashMap<>();
		map.put(Supplier.class.getSimpleName(), RelationType.SUPPLIER);
		map.put(Contact.class.getSimpleName(), RelationType.CONTACT);
		map.put(Task.class.getSimpleName(), RelationType.TASK);
		map.put(Document.class.getSimpleName(), RelationType.DOCUMENT);
		map.put(TaskLog.class.getSimpleName(), RelationType.TASK_LOG);
		map.put(Register.class.getSimpleName(), RelationType.REGISTER);
		map.put(Asset.class.getSimpleName(), RelationType.ASSET);
		map.put(StandardSection.class.getSimpleName(), RelationType.STANDARD_SECTION);
		map.put(ThreatAssessment.class.getSimpleName(), RelationType.THREAT_ASSESSMENT);
		map.put(ThreatAssessmentResponse.class.getSimpleName(), RelationType.THREAT_ASSESSMENT_RESPONSE);
		map.put(Precaution.class.getSimpleName(), RelationType.PRECAUTION);
		map.put(DBSAsset.class.getSimpleName(), RelationType.DBSASSET);
		map.put(DBSOversight.class.getSimpleName(), RelationType.DBSOVERSIGHT);
		map.put(Incident.class.getSimpleName(), RelationType.INCIDENT);
		map.put(DPIA.class.getSimpleName(), RelationType.DPIA);
		return map;
	}

	private String formatBrokenRelation(Relation relation, RelationType type) {
		Long missingId = null;

		if (type.equals(relation.getRelationAType())) {
			missingId = relation.getRelationAId();
		}
		else if (type.equals(relation.getRelationBType())) {
			missingId = relation.getRelationBId();
		}

		return String.format("\t{ relation Id: %d - relates to %s %d }",
				relation.getId(), type, missingId);
	}
}

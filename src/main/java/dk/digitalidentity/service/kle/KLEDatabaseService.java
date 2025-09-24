package dk.digitalidentity.service.kle;

import dk.digitalidentity.model.entity.kle.KLEGroup;
import dk.digitalidentity.model.entity.kle.KLEKeyword;
import dk.digitalidentity.model.entity.kle.KLELegalReference;
import dk.digitalidentity.model.entity.kle.KLEMainGroup;
import dk.digitalidentity.model.entity.kle.KLESubject;
import dk.digitalidentity.model.entity.kle.KLESyncableService;
import dk.digitalidentity.model.entity.kle.Syncable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional
@Slf4j
@Service
@RequiredArgsConstructor
public class KLEDatabaseService {
	private final KLEMainGroupService kLEMainGroupService;
	private final KLEGroupService kLEGroupService;
	private final KLESubjectService kLESubjectService;
	private final KLELegalReferenceService kLELegalReferenceService;
	private final KLEKeywordService kleKeywordService;

	public record DifferenceHolder<I>(Set<I> toCreate, Set<I> toDelete, Set<I> toUpdate) {
	}

	protected static class ContextCache {
		Map<String, KLEKeyword> keywords = new ConcurrentHashMap<>();
		Map<String, KLELegalReference> legalReferences = new ConcurrentHashMap<>();
		Map<String, KLESubject> subjects = new ConcurrentHashMap<>();
		Map<String, KLEGroup> groups = new ConcurrentHashMap<>();
	}

	@FunctionalInterface
	public interface TriFunction<T, U, V, R> {
		R apply(T t, U u, V v);
	}

	@Transactional
	public void syncKeywords(final Map<String, KLEKeyword> allImportedKLEKeywords) {
		ContextCache contextCache = new ContextCache();
		sync(allImportedKLEKeywords, kleKeywordService, this::updateKeyword, this::createBlankPersistedKeywords, contextCache);
		log.info("Synced {} keywords", allImportedKLEKeywords.size());
	}

	@Transactional
	public void syncLegalReferences(final Map<String, KLELegalReference> allImportedKLELegalReferences) {
		ContextCache contextCache = new ContextCache();
		sync(allImportedKLELegalReferences, kLELegalReferenceService, this::updateLegalReference, this::createBlankPersistedLegalrefs, contextCache);
		log.info("Synced {} legal references", allImportedKLELegalReferences.size());
	}

	@Transactional
	public void syncSubjects(final Map<String, KLESubject> allImportedKLESubjects) {
		ContextCache contextCache = new ContextCache();
		Set<String> importedKeywordIds = allImportedKLESubjects.values().stream().flatMap(s -> s.getKeywords().stream().map(KLEKeyword::getId)).collect(Collectors.toSet());
		contextCache.keywords = kleKeywordService.findAllById(importedKeywordIds).stream().collect(Collectors.toMap(KLEKeyword::getId, Function.identity()));

		Set<String> importedLegalRefIds = allImportedKLESubjects.values().stream().flatMap(s -> s.getLegalReferences().stream().map(KLELegalReference::getId)).collect(Collectors.toSet());
		contextCache.legalReferences = kLELegalReferenceService.findAllById(importedLegalRefIds).stream().collect(Collectors.toMap(KLELegalReference::getId, Function.identity()));

		sync(allImportedKLESubjects, kLESubjectService, this::updateSubject, this::createBlankPersistedSubject, contextCache);
		log.info("Synced {} subjects", allImportedKLESubjects.size());
	}

	@Transactional
	public void syncGroups(final Map<String, KLEGroup> allImportedKLEGroups) {
		ContextCache contextCache = new ContextCache();

		Set<String> importedKeywordIds = allImportedKLEGroups.values().stream().flatMap(s -> s.getKeywords().stream().map(KLEKeyword::getId)).collect(Collectors.toSet());
		contextCache.keywords = kleKeywordService.findAllById(importedKeywordIds).stream().collect(Collectors.toMap(KLEKeyword::getId, Function.identity()));

		Set<String> importedLegalRefIds = allImportedKLEGroups.values().stream().flatMap(s -> s.getLegalReferences().stream().map(KLELegalReference::getId)).collect(Collectors.toSet());
		contextCache.legalReferences = kLELegalReferenceService.findAllById(importedLegalRefIds).stream().collect(Collectors.toMap(KLELegalReference::getId, Function.identity()));

		Set<String> importedSubjectIds = allImportedKLEGroups.values().stream().flatMap(g -> g.getSubjects().stream().map(KLESubject::getId)).collect(Collectors.toSet());
		contextCache.subjects = kLESubjectService.findAllById(importedSubjectIds).stream().collect(Collectors.toMap(KLESubject::getId, Function.identity()));

		sync(allImportedKLEGroups, kLEGroupService, this::updateGroup, this::createBlankPersistedGroup, contextCache);
		log.info("Synced {} groups", allImportedKLEGroups.size());
	}

	@Transactional
	public void syncMaingroups(final Map<String, KLEMainGroup> allImportedMainGroups) {
		ContextCache contextCache = new ContextCache();
		sync(allImportedMainGroups, kLEMainGroupService, this::updateMainGroup, this::createBlankPersistedMainGroup, contextCache);
		log.info("Synced {} maingroups", allImportedMainGroups.size());
	}

	protected <T extends Syncable<ID>, ID> void sync(Map<ID, T> importedKLEObjects, KLESyncableService<T, ID> service, TriFunction<T, T, ContextCache, T> updateFunction, Function<Collection<ID>, Collection<T>> createFunction, ContextCache contextCache) {
		DifferenceHolder<ID> differenceHolder = determineDifferences(
				importedKLEObjects.keySet(),
				service.findAllIds());

		// Create new entities
		Set<T> savedBlankNewEntities = new HashSet<>(createFunction.apply(differenceHolder.toCreate));
		log.info("{} created entities", savedBlankNewEntities.size());

		// Delete entities
		service.deleteAllById(differenceHolder.toDelete);
		log.info("{} deleted entities", differenceHolder.toDelete.size());

		// Update entities (including the blank created)
		Map<ID, T> toUpdate = Stream.concat(
				service.findAllById(differenceHolder.toUpdate).stream(),
				savedBlankNewEntities.stream()
		).collect(Collectors.toMap(Syncable::getId, Function.identity()));

		Set<T> updatedEntities = new HashSet<>();
		for (Map.Entry<ID, T> entry : toUpdate.entrySet()) {
			updatedEntities.add(updateFunction.apply(entry.getValue(), importedKLEObjects.get(entry.getKey()), contextCache));
		}
		service.saveAllSyncables(updatedEntities);
		log.info("{} updated entities", differenceHolder.toUpdate.size());
	}

	private <T> DifferenceHolder<T> determineDifferences(Set<T> importedSet, Set<T> databaseSet) {
		// Elements in imports but not in db
		Set<T> toCreate = new HashSet<>(importedSet);
		toCreate.removeAll(databaseSet);

		// Elements in db but not in imports
		Set<T> toDelete = new HashSet<>(databaseSet);
		toDelete.removeAll(importedSet);

		// Intersection of elements
		Set<T> toUpdate = new HashSet<>(importedSet);
		toUpdate.retainAll(databaseSet);

		return new DifferenceHolder<>(toCreate, toDelete, toUpdate);
	}

	public KLEMainGroup updateMainGroup(KLEMainGroup existing, KLEMainGroup imported, ContextCache contextCache) {
		existing.setDeleted(false);
		existing.setCreationDate(imported.getCreationDate());
		existing.setTitle(imported.getTitle());
		existing.setLastUpdateDate(imported.getLastUpdateDate());
		existing.setInstructionText(imported.getInstructionText());
		existing.setUuid(imported.getUuid());

		updateMainGroupAssociations(existing, imported, contextCache);

		return existing;
	}

	public KLEGroup updateGroup(KLEGroup existing, KLEGroup imported, ContextCache contextCache) {
		existing.setDeleted(false);
		existing.setCreationDate(imported.getCreationDate());
		existing.setTitle(imported.getTitle());
		existing.setLastUpdateDate(imported.getLastUpdateDate());
		existing.setInstructionText(imported.getInstructionText());
		existing.setUuid(imported.getUuid());

		updateGroupAssociations(existing, imported, contextCache);

		return existing;
	}

	private void updateMainGroupAssociations(KLEMainGroup existing, KLEMainGroup imported, ContextCache contextCache) {
		existing.getKleGroups().clear();
		Set<KLEGroup> groups = new HashSet<>(kLEGroupService.findAllById(imported.getKleGroups().stream().map(KLEGroup::getId).collect(Collectors.toSet())));
		existing.getKleGroups().clear();
		for (KLEGroup group : groups) {
			group.setMainGroup(existing);
			existing.getKleGroups().add(group);
		}
	}

	public KLESubject updateSubject(KLESubject existing, KLESubject imported, ContextCache contextCache) {
		existing.setDeleted(false);
		existing.setCreationDate(imported.getCreationDate());
		existing.setTitle(imported.getTitle());
		existing.setDurationBeforeDeletion(imported.getDurationBeforeDeletion());
		existing.setPreservationCode(imported.getPreservationCode());
		existing.setLastUpdateDate(imported.getLastUpdateDate());
		existing.setInstructionText(imported.getInstructionText());
		existing.setUuid(imported.getUuid());

		updateSubjectAssociations(existing, imported, contextCache);
		return existing;
	}

	private void updateGroupAssociations(KLEGroup existing, KLEGroup imported, ContextCache contextCache) {
		existing.getKeywords().clear();
		if (!imported.getKeywords().isEmpty()) {
			Set<KLEKeyword> keywords = imported.getKeywords().stream()
					.map(imp -> contextCache.keywords.get(imp.getId()))
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

			existing.setKeywords(keywords);
		}

		existing.getLegalReferences().clear();
		if (!imported.getLegalReferences().isEmpty()) {
			Set<KLELegalReference> legalReferences = imported.getLegalReferences().stream()
					.map(imp -> contextCache.legalReferences.get(imp.getId()))
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

			existing.setLegalReferences(legalReferences);
		}

		existing.getSubjects().clear();
		if (!imported.getSubjects().isEmpty()) {
			Set<KLESubject> subjects = imported.getSubjects().stream()
					.map(sub -> contextCache.subjects.get(sub.getId()))
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

			existing.getSubjects().clear();
			for (KLESubject subject : subjects) {
				existing.getSubjects().add(subject);
				subject.setGroup(existing);
			}
		}
	}

	private void updateSubjectAssociations(KLESubject existing, KLESubject imported, ContextCache contextCache) {
		existing.getKeywords().clear();
		if (!imported.getKeywords().isEmpty()) {
			Set<KLEKeyword> keywords = imported.getKeywords().stream()
					.map(imp -> contextCache.keywords.get(imp.getId()))
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

			existing.setKeywords(keywords);
		}

		existing.getLegalReferences().clear();
		if (!imported.getLegalReferences().isEmpty()) {
			Set<KLELegalReference> legalReferences = imported.getLegalReferences().stream()
					.map(imp -> contextCache.legalReferences.get(imp.getId()))
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

			existing.setLegalReferences(legalReferences);
		}
	}

	private KLELegalReference updateLegalReference(KLELegalReference existing, KLELegalReference imported, ContextCache contextCache) {
		existing.setDeleted(false);
		existing.setUrl(imported.getUrl());
		existing.setTitle(imported.getTitle());
		existing.setParagraph(imported.getParagraph());
		return existing;
	}

	private KLEKeyword updateKeyword(KLEKeyword existing, KLEKeyword imported, ContextCache contextCache) {
		existing.setText(imported.getText());
		existing.setHandlingsfacetNr(imported.getHandlingsfacetNr());
		return existing;
	}

	private Collection<KLEKeyword> createBlankPersistedKeywords (Collection<String> importedIds) {
		return importedIds.stream().map(id ->  KLEKeyword.builder()
					.hashedId(id)
					.isNew(true)
					.build())
				.collect(Collectors.toSet());
	}

	private Collection<KLELegalReference> createBlankPersistedLegalrefs (Collection<String> importedIds) {
		return importedIds.stream().map(id ->  KLELegalReference.builder()
						.accessionNumber(id)
						.isNew(true)
						.build())
				.collect(Collectors.toSet());
	}

	private Collection<KLESubject> createBlankPersistedSubject (Collection<String> importedIds) {
		return importedIds.stream().map(id ->  KLESubject.builder()
						.subjectNumber(id)
						.isNew(true)
						.build())
				.collect(Collectors.toSet());
	}

	private Collection<KLEGroup> createBlankPersistedGroup (Collection<String> importedIds) {
		return importedIds.stream().map(id ->  KLEGroup.builder()
						.groupNumber(id)
						.isNew(true)
						.build())
				.collect(Collectors.toSet());
	}

	private Collection<KLEMainGroup> createBlankPersistedMainGroup (Collection<String> importedIds) {
		return importedIds.stream().map(id ->  KLEMainGroup.builder()
						.mainGroupNumber(id)
						.isNew(true)
						.build())
				.collect(Collectors.toSet());
	}

}

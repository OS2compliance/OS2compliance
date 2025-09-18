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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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

	@Transactional
	public void syncKeywords(final Map<String, KLEKeyword> allImportedKLEKeywords) {
		sync(allImportedKLEKeywords, kleKeywordService, this::updateKeyword);
	}

	@Transactional
	public void syncLegalReferences(final Map<String, KLELegalReference> allImportedKLELegalReferences) {
		sync(allImportedKLELegalReferences, kLELegalReferenceService, this::updateLegalReference);
	}

	@Transactional
	public void syncSubjects(final Map<String, KLESubject> allImportedKLESubjects) {
		sync(allImportedKLESubjects, kLESubjectService, this::updateSubject);
	}

	@Transactional
	public void syncGroups(final Map<String, KLEGroup> allImportedKLEGroups) {
		sync(allImportedKLEGroups, kLEGroupService, this::updateGroup);
	}

	@Transactional
	public void syncMaingroups(final Map<String, KLEMainGroup> allImportedMainGroups) {
		sync(allImportedMainGroups, kLEMainGroupService, this::updateMainGroup);
	}

	protected <T extends Syncable<ID>, ID> void sync(Map<ID, T> allImportedKLEObjects, KLESyncableService<T, ID> service, BiConsumer<T, T> updateFunction) {
		DifferenceHolder<ID> differenceHolder = determineDifferences(
				allImportedKLEObjects.keySet(),
				service.findAllIds());

		// Create new entities
		Set<T> toCreate = differenceHolder.toCreate.stream().map(id -> {
			T entity = allImportedKLEObjects.get(id);
			entity.markAsExisting();
			return entity;
		}).collect(Collectors.toSet());
		service.saveAllSyncables(toCreate);

		// Delete entities
		service.deleteAllById(differenceHolder.toDelete);

		// Update entities
		Map<ID, T> existingEntities = service.findAllById(differenceHolder.toUpdate).stream()
				.collect(Collectors.toMap(Syncable::getId, Function.identity()));

		for (ID id : differenceHolder.toUpdate) {
			updateFunction.accept(existingEntities.get(id), allImportedKLEObjects.get(id));
		}
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

	public void updateMainGroup(KLEMainGroup existing, KLEMainGroup imported) {
		existing.setDeleted(false);
		existing.setCreationDate(imported.getCreationDate());
		existing.setTitle(imported.getTitle());
		existing.setLastUpdateDate(imported.getLastUpdateDate());
		existing.setInstructionText(imported.getInstructionText());
		existing.setUuid(imported.getUuid());

		updateMainGroupAssociations(existing, imported);
	}

	public void updateGroup(KLEGroup existing, KLEGroup imported) {
		existing.setDeleted(false);
		existing.setCreationDate(imported.getCreationDate());
		existing.setTitle(imported.getTitle());
		existing.setLastUpdateDate(imported.getLastUpdateDate());
		existing.setInstructionText(imported.getInstructionText());
		existing.setUuid(imported.getUuid());

		updateGroupAssociations(existing, imported);
	}

	private void updateMainGroupAssociations(KLEMainGroup existing, KLEMainGroup imported) {
		// Remove existing relationships
		for (KLEGroup group : new ArrayList<>(existing.getKleGroups())) {
			group.setMainGroup(null);
			existing.getKleGroups().remove(group);
		}

		// Add new relationships
		if (imported.getKleGroups() != null) {
			Set<String> importedGroupIds = imported.getKleGroups().stream()
					.map(KLEGroup::getGroupNumber)
					.collect(Collectors.toSet());

			List<KLEGroup> groups = kLEGroupService.findAllById(importedGroupIds);
			for (KLEGroup group : groups) {
				group.setMainGroup(existing);
				existing.getKleGroups().add(group);
			}
		}
	}

	private void updateGroupAssociations(KLEGroup existing, KLEGroup imported) {
		// Remove existing relations
		for (KLEKeyword keyword : new ArrayList<>(existing.getKeywords())) {
			keyword.getGroups().remove(existing);
			existing.getKeywords().remove(keyword);
		}
		// Add new relations
		Set<KLEKeyword> keywords = kleKeywordService.findAllById(imported.getKeywords().stream().map(KLEKeyword::getId).collect(Collectors.toSet()));
		for (KLEKeyword keyword : keywords) {
			existing.getKeywords().add(keyword);
			keyword.getGroups().add(existing);
		}

		// Remove existing relations
		for (KLELegalReference legalReference : new ArrayList<>(existing.getLegalReferences())) {
			legalReference.getGroups().remove(existing);
			existing.getLegalReferences().remove(legalReference);
		}
		// Add new relations
		List<KLELegalReference> legalRefs = kLELegalReferenceService.findAllById(imported.getLegalReferences().stream().map(KLELegalReference::getId).collect(Collectors.toSet()));
		for (KLELegalReference legalReference : legalRefs) {
			existing.getLegalReferences().add(legalReference);
			legalReference.getGroups().add(existing);
		}

		// Remove existing relationships
		for (KLESubject subject : new ArrayList<>(existing.getSubjects())) {
			subject.setGroup(null);
			existing.getSubjects().remove(subject);
		}

		// Add new relationships
		List<KLESubject> subjects = kLESubjectService.findAllById(imported.getSubjects().stream().map(KLESubject::getSubjectNumber).collect(Collectors.toSet()));
		for (KLESubject subject : subjects) {
			existing.getSubjects().add(subject);
			subject.setGroup(existing);
		}
	}

	public void updateSubject(KLESubject existing, KLESubject imported) {
		existing.setDeleted(false);
		existing.setCreationDate(imported.getCreationDate());
		existing.setTitle(imported.getTitle());
		existing.setDurationBeforeDeletion(imported.getDurationBeforeDeletion());
		existing.setPreservationCode(imported.getPreservationCode());
		existing.setLastUpdateDate(imported.getLastUpdateDate());
		existing.setInstructionText(imported.getInstructionText());
		existing.setUuid(imported.getUuid());

		updateSubjectAssociations(existing, imported.getKeywords().stream().map(KLEKeyword::getId).collect(Collectors.toSet()), imported.getLegalReferences().stream().map(KLELegalReference::getId).collect(Collectors.toSet()));
	}

	private void updateSubjectAssociations(KLESubject existing, Set<String> keywordIds, Set<String> legalRefIds) {
		// Remove existing relations
		for (KLEKeyword keyword : new ArrayList<>(existing.getKeywords())) {
			keyword.getSubjects().remove(existing);
			existing.getKeywords().remove(keyword);
		}
		// Add new relations
		Set<KLEKeyword> keywords = kleKeywordService.findAllById(keywordIds);
		for (KLEKeyword keyword : keywords) {
			keyword.getSubjects().add(existing);
			existing.getKeywords().add(keyword);
		}

		// Remove existing relations
		for (KLELegalReference legalReference : new ArrayList<>(existing.getLegalReferences())) {
			legalReference.getSubjects().remove(existing);
			existing.getLegalReferences().remove(legalReference);
		}
		// Add new relations
		List<KLELegalReference> legalRefs = kLELegalReferenceService.findAllById(legalRefIds);
		for (KLELegalReference legalReference : legalRefs) {
			legalReference.getSubjects().add(existing);
			existing.getLegalReferences().add(legalReference);
		}
	}

	private void updateLegalReference(KLELegalReference existing, KLELegalReference imported) {
		existing.setDeleted(false);
		existing.setUrl(imported.getUrl());
		existing.setTitle(imported.getTitle());
		existing.setParagraph(imported.getParagraph());
	}

	private void updateKeyword(KLEKeyword existing, KLEKeyword imported) {
		existing.setText(imported.getText());
		existing.setHandlingsfacetNr(imported.getHandlingsfacetNr());
	}

}

package dk.digitalidentity.service.kle;

import dk.digitalidentity.model.entity.kle.KLEGroup;
import dk.digitalidentity.model.entity.kle.KLEKeyword;
import dk.digitalidentity.model.entity.kle.KLELegalReference;
import dk.digitalidentity.model.entity.kle.KLEMainGroup;
import dk.digitalidentity.model.entity.kle.KLESubject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

	public void syncWithDatabase(
			final Map<String, KLEMainGroup> mainGroupCache,
			final Map<String, KLEGroup> groupCache,
			final Map<String, KLESubject> subjectCache,
			final Map<String, KLELegalReference> kleLegalReferenceCache,
			final Map<String, KLEKeyword> keywordCache
	) {
		Collection<KLELegalReference> kleLegalReferences = kLELegalReferenceService.getAll();
		Collection<KLEKeyword> kleKeywords = kleKeywordService.getAll();
		Collection<KLESubject> kleSubjects = kLESubjectService.getAll();
		Collection<KLEMainGroup> kleMainGroups = kLEMainGroupService.getAll();
		Collection<KLEGroup> kleGroups = kLEGroupService.getAll();

		syncKeywords(keywordCache, kleKeywords);
		syncLegalReferences(kleLegalReferenceCache, kleLegalReferences);
		syncSubjects(subjectCache, kleSubjects);
		syncGroups(groupCache, kleGroups);
		syncMainGroups(mainGroupCache, kleMainGroups);
	}

	public void syncMainGroups(final Map<String, KLEMainGroup> cache, Collection<KLEMainGroup> allExisting) {
		Map<String, KLEMainGroup> existingGroups = allExisting.stream()
				.collect(Collectors.toMap(KLEMainGroup::getMainGroupNumber, g -> g));

		List<KLEMainGroup> toSave = new ArrayList<>();

		// Process all cached groups
		for (KLEMainGroup cached : cache.values()) {
			KLEMainGroup existing = existingGroups.get(cached.getMainGroupNumber());

			if (existing != null) {
				// Update existing
				updateMainGroup(existing, cached);
				existing.markAsExisting(); // Ensure it's not treated as new
				toSave.add(existing);
				existingGroups.remove(cached.getMainGroupNumber());
			} else {
				// New
				cached.setAsNew();
				toSave.add(cached);
			}
		}

		// Mark remaining as deleted
		existingGroups.values().forEach(g -> {
			g.setDeleted(true);
			g.markAsExisting();
			toSave.add(g);
		});

		kLEMainGroupService.saveAll(toSave);
	}

	public void syncGroups(final Map<String, KLEGroup> cache, Collection<KLEGroup> allExisting) {
		Map<String, KLEGroup> existingGroups = allExisting.stream()
				.collect(Collectors.toMap(KLEGroup::getGroupNumber, g -> g));

		List<KLEGroup> toSave = new ArrayList<>();

		// Process all cached groups
		for (KLEGroup cached : cache.values()) {
			KLEGroup existing = existingGroups.get(cached.getGroupNumber());

			if (existing != null) {
				// Update existing
				updateGroup(existing, cached);
				existing.markAsExisting(); // Ensure it's not treated as new
				toSave.add(existing);
				existingGroups.remove(cached.getGroupNumber());
			} else {
				// New
				cached.setAsNew();
				toSave.add(cached);
			}
		}

		// Mark remaining as deleted
		existingGroups.values().forEach(g -> {
			g.setDeleted(true);
			g.markAsExisting();
			toSave.add(g);
		});

		kLEGroupService.saveAll(toSave);
	}

	public void syncSubjects(final Map<String, KLESubject> cache, Collection<KLESubject> allExisting) {
		Map<String, KLESubject> existingGroups = allExisting.stream()
				.collect(Collectors.toMap(KLESubject::getSubjectNumber, g -> g));

		List<KLESubject> toSave = new ArrayList<>();

		// Process all cached groups
		for (KLESubject cached : cache.values()) {
			KLESubject existing = existingGroups.get(cached.getSubjectNumber());

			if (existing != null) {
				// Update existing
				updateSubject(existing, cached);
				existing.markAsExisting(); // Ensure it's not treated as new
				toSave.add(existing);
				existingGroups.remove(cached.getSubjectNumber());
			} else {
				// New
				cached.setAsNew();
				toSave.add(cached);
			}
		}

		// Mark remaining as deleted
		existingGroups.values().forEach(g -> {
			g.setDeleted(true);
			g.markAsExisting();
			toSave.add(g);
		});

		kLESubjectService.saveAll(toSave);
	}

	public void syncLegalReferences(final Map<String, KLELegalReference> cache, Collection<KLELegalReference> allExisting) {
		Map<String, KLELegalReference> existingGroups = allExisting.stream()
				.collect(Collectors.toMap(KLELegalReference::getAccessionNumber, g -> g));

		List<KLELegalReference> toSave = new ArrayList<>();

		// Process all cached groups
		for (KLELegalReference cached : cache.values()) {
			KLELegalReference existing = existingGroups.get(cached.getAccessionNumber());

			if (existing != null) {
				// Update existing
				updateLegalReference(existing, cached);
				existing.markAsExisting(); // Ensure it's not treated as new
				toSave.add(existing);
				existingGroups.remove(cached.getAccessionNumber());
			} else {
				// New
				cached.setAsNew();
				toSave.add(cached);
			}
		}

		// Mark remaining as deleted
		existingGroups.values().forEach(g -> {
			g.setDeleted(true);
			g.markAsExisting();
			toSave.add(g);
		});
		kLELegalReferenceService.saveAll(toSave);

	}

	public void syncKeywords(final Map<String, KLEKeyword> cache, Collection<KLEKeyword> allExisting) {
		Map<String, KLEKeyword> existingGroups = allExisting.stream()
				.collect(Collectors.toMap(KLEKeyword::getHashedId, g -> g));

		List<KLEKeyword> toSave = new ArrayList<>();

		// Process all cached groups
		for (KLEKeyword cached : cache.values()) {
			KLEKeyword existing = existingGroups.get(cached.getHashedId());

			if (existing != null) {
				// Update existing
				updateKeyword(existing, cached);
				existing.markAsExisting(); // Ensure it's not treated as new
				toSave.add(existing);
				existingGroups.remove(cached.getHashedId());
			} else {
				// New
				cached.setAsNew();
				toSave.add(cached);
			}
		}

		// Mark remaining as deleted
		kleKeywordService.deleteAll(existingGroups.values());

		kleKeywordService.saveAll(toSave);
	}

	private void updateGroup(KLEGroup existing, KLEGroup cached) {
		existing.setDeleted(false);
		existing.setCreationDate(cached.getCreationDate());
		existing.setTitle(cached.getTitle());
		existing.setLastUpdateDate(cached.getLastUpdateDate());
		existing.setInstructionText(cached.getInstructionText());
		existing.setUuid(cached.getUuid());

		existing.setKeywords(cached.getKeywords());
		for (KLEKeyword keyword : cached.getKeywords()) {
			existing.getKeywords().add(keyword);
		}
		existing.setLegalReferences(cached.getLegalReferences());
		for (KLELegalReference legalReference : cached.getLegalReferences()) {
			existing.getLegalReferences().add(legalReference);
		}
		existing.setSubjects(cached.getSubjects());
		for (KLESubject subject : cached.getSubjects()) {
			existing.getSubjects().add(subject);
		}
	}

	private void updateMainGroup(KLEMainGroup existing, KLEMainGroup cached) {
		existing.setDeleted(false);
		existing.setCreationDate(cached.getCreationDate());
		existing.setTitle(cached.getTitle());
		existing.setLastUpdateDate(cached.getLastUpdateDate());
		existing.setInstructionText(cached.getInstructionText());
		existing.setUuid(cached.getUuid());

		existing.setKleGroups(cached.getKleGroups());
		for (KLEGroup group : cached.getKleGroups()) {
			group.setMainGroup(existing);
		}
	}

	private void updateSubject(KLESubject existing, KLESubject cached) {
		existing.setDeleted(false);
		existing.setCreationDate(cached.getCreationDate());
		existing.setTitle(cached.getTitle());
		existing.setDurationBeforeDeletion(cached.getDurationBeforeDeletion());
		existing.setPreservationCode(cached.getPreservationCode());
		existing.setLastUpdateDate(cached.getLastUpdateDate());
		existing.setInstructionText(cached.getInstructionText());
		existing.setUuid(cached.getUuid());

		updateSubjectAssociations(existing, cached);
	}

	private void updateSubjectAssociations(KLESubject existing, KLESubject cached) {
		existing.setKeywords(cached.getKeywords());
		for (KLEKeyword keyword : cached.getKeywords()) {
			keyword.getSubjects().add(existing);
		}
		existing.setLegalReferences(cached.getLegalReferences());
		for (KLELegalReference legalReference : cached.getLegalReferences()) {
			legalReference.getSubjects().add(existing);
		}
	}

	private void updateLegalReference(KLELegalReference existing, KLELegalReference cached) {
		existing.setDeleted(false);
		existing.setUrl(cached.getUrl());
		existing.setTitle(cached.getTitle());
		existing.setParagraph(cached.getParagraph());
	}

	private void updateKeyword(KLEKeyword existing, KLEKeyword cached) {
		existing.setText(cached.getText());
		existing.setHandlingsfacetNr(cached.getHandlingsfacetNr());
	}

}

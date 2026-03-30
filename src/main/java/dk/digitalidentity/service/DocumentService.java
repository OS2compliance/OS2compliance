package dk.digitalidentity.service;

import dk.digitalidentity.dao.DocumentDao;
import dk.digitalidentity.dao.grid.DocumentGridDao;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.grid.DocumentGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.tag.TagableService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static dk.digitalidentity.Constants.ASSOCIATED_DOCUMENT_PROPERTY;
import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

@Service
public class DocumentService implements TagableService<Document> {

	private final DocumentGridDao documentGridDao;
	private final DocumentDao documentDao;
    private final TaskService taskService;
    private final RelationService relationService;
    private final UserService userService;

	public DocumentService(final DocumentDao documentDao, final TaskService taskService, final RelationService relationService, final UserService userService, DocumentGridDao documentGridDao) {
		this.documentDao = documentDao;
        this.taskService = taskService;
        this.relationService = relationService;
        this.userService = userService;
		this.documentGridDao = documentGridDao;
    }

	public boolean isResponsibleFor(Document document) {
		return (document.getResponsibleUser() != null
				&& SecurityUtil.getPrincipalUuid().equals(document.getResponsibleUser().getUuid())
		);
	}

	public Optional<Document> get(final Long id) {
		return documentDao.findById(id);
	}

	public Page<Document> getPaged(final int pageSize, final int page) {
		return documentDao.findAll(Pageable.ofSize(pageSize).withPage(page));
	}

	public List<Document> getAll() {
		return documentDao.findAll();
	}
    @Transactional
	public Document create(final Document document) {
        return documentDao.save(document);
	}

    @Transactional
	public void update(final Document document, boolean includeInYearWheel) {
        updateAssociatedCheck(document, includeInYearWheel);
		documentDao.saveAndFlush(document);
	}

    @Transactional
	public void delete(final Document document) {
		relationService.deleteRelatedTo(document.getId());
		documentDao.delete(document);
	}

    @Transactional
    public void deleteById(final Long id) {
		relationService.deleteRelatedTo(id);
        documentDao.deleteById(id);
    }

    @Transactional
    public void updateAssociatedCheck(final Document document, boolean includeInYearWheel) {
		Task task = findRelatedCheckTask(document, relationService);
		if (task != null) {
			task.setIncludeInReport(includeInYearWheel);
            if (document.getNextRevision() != null) {
                task.setNextDeadline(document.getNextRevision());
            } else {
                task.setNextDeadline(LocalDate.of(2099, 1,1));
            }
            setTaskRevisionInterval(document, task);
        }
    }

	public Task findRelatedCheckTask(Document document, RelationService relationService) {
		final List<Relatable> relatedTasks = relationService.findAllRelatedTo(document);
		return relatedTasks.stream()
			.filter(r -> r.getRelationType() == RelationType.TASK && r.getProperties().stream()
				.anyMatch(p -> ASSOCIATED_DOCUMENT_PROPERTY.equals(p.getKey()))
			).findFirst().map(Task.class::cast).orElse(null);
	}

	@Transactional
    public void createAssociatedCheck(final Document document, boolean includeInYearWheel) {
        if (document.getNextRevision() == null) {
            return;
        }
        final Task task = new Task();
        task.setTaskType(TaskType.CHECK);
        task.setName("Revision af " + document.getName());
        task.setCreatedAt(LocalDateTime.now());
        task.setNextDeadline(document.getNextRevision());
        task.setNotifyResponsible(false);
		task.setIncludeInReport(includeInYearWheel);
		task.setResponsibleUsers(document.getResponsibleUser() != null ? Set.of(document.getResponsibleUser()) : Set.of(userService.currentUser()));
        task.setDescription("Revider dokumentet " + document.getName());
        task.getProperties().add(Property.builder()
            .entity(task)
            .key(ASSOCIATED_DOCUMENT_PROPERTY)
            .value("" + document.getId())
            .build()
        );
        setTaskRevisionInterval(document, task);
        final Task savedTask = taskService.saveTask(task);
        relationService.addRelation(savedTask, document);
    }

	@Override
	@Transactional
	public Tag addTag(Long entityId, Tag tag) {
		Document entity = documentDao.findById(entityId)
				.orElseThrow(() -> new EntityNotFoundException(Document.class.getSimpleName() + " not found with id: " + entityId));

		entity.getTags().add(tag);
		documentDao.save(entity);

		return tag;
	}

	@Override
	@Transactional
	public Tag removeTag(Long entityId, Long tagId) {
		Document entity = documentDao.findById(entityId)
				.orElseThrow(() -> new EntityNotFoundException(Document.class.getSimpleName() + " not found with id: " + entityId));

		Set<Tag> tags = entity.getTags();
		Tag tag = tags.stream().filter(t -> t.getId() == tagId).findAny().orElse(null);
		if (tag != null) {
			entity.getTags().remove(tag);
			documentDao.save(entity);
		}
		return tag;
	}

	@Override
	public Class<Document> getEntityType() {
		return Document.class;
	}

	@Override
	public Set<Tag> findTagsByEntityId(Long entityId) {
		return documentDao.findTagsByEntityId(entityId);
	}

	@Override
	public Set<Tag> findTagsByEntityIds(Collection<Long> entityIds) {
		return documentDao.findTagsByEntityIds(entityIds);
	}

    private static void setTaskRevisionInterval(final Document document, final Task task) {
        switch(document.getRevisionInterval()) {
            case YEARLY -> task.setRepetition(TaskRepetition.YEARLY);
            case HALF_YEARLY -> task.setRepetition(TaskRepetition.HALF_YEARLY);
            case EVERY_SECOND_YEAR -> task.setRepetition(TaskRepetition.EVERY_SECOND_YEAR);
            case EVERY_THIRD_YEAR -> task.setRepetition(TaskRepetition.EVERY_THIRD_YEAR);
            case NONE -> task.setRepetition(TaskRepetition.NONE);
        }
    }

	public Page<DocumentGrid> getDocuments(String sortColumn, String sortDirection, Map<String, String> filters, int page, int pageLimit, User user) {
		Page<DocumentGrid> documents;
		if (SecurityUtil.isOperationAllowed(Roles.READ_ALL)) {
			documents = documentGridDao.findAllWithColumnSearch(
					validateSearchFilters(filters, DocumentGrid.class),
					buildPageable(page, pageLimit, sortColumn, sortDirection),
					DocumentGrid.class
			);
		} else {
			documents = documentGridDao.findAllWithAssignedUser(
					validateSearchFilters(filters, DocumentGrid.class),
					user,
					buildPageable(page, pageLimit, sortColumn, sortDirection),
					DocumentGrid.class
			);
		}
		return documents;
	}

	public boolean isInUseOnDocument(Long id) {
		return documentDao.existsByDocumentTypeId(id);
	}

	public List<Document> findByIds(List<Long> ids, User user) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}

		// Fetch all documents by IDs
		List<Document> documents = documentDao.findAllById(ids);

		// Apply same security filtering as in the grid
		if (SecurityUtil.isOperationAllowed(Roles.READ_ALL)) {
			// User can read all documents
			return documents;
		} else {
			// User can only read documents where they are responsible
			return documents.stream()
					.filter(doc -> doc.getResponsibleUser() != null &&
							doc.getResponsibleUser().getUuid().equals(user.getUuid()))
					.toList();
		}
	}

}

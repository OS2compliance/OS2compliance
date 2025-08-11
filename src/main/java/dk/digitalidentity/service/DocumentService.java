package dk.digitalidentity.service;

import dk.digitalidentity.dao.DocumentDao;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.security.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static dk.digitalidentity.Constants.ASSOCIATED_DOCUMENT_PROPERTY;

@Service
public class DocumentService {

	private final DocumentDao documentDao;
    private final TaskService taskService;
    private final RelationService relationService;
    private final UserService userService;

	public DocumentService(final DocumentDao documentDao, final TaskService taskService, final RelationService relationService, final UserService userService) {
		this.documentDao = documentDao;
        this.taskService = taskService;
        this.relationService = relationService;
        this.userService = userService;
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
	public void update(final Document document) {
        updateAssociatedCheck(document);
		documentDao.saveAndFlush(document);
	}

    @Transactional
	public void delete(final Document document) {
		documentDao.delete(document);
	}

    @Transactional
    public void deleteById(final Long id) {
        documentDao.deleteById(id);
    }

    @Transactional
    public void updateAssociatedCheck(final Document document) {
        final List<Relatable> relatedTasks = relationService.findAllRelatedTo(document);
        final Task task = relatedTasks.stream()
            .filter(r -> r.getRelationType() == RelationType.TASK && r.getProperties().stream()
                .anyMatch(p -> ASSOCIATED_DOCUMENT_PROPERTY.equals(p.getKey()))
            ).findFirst().map(Task.class::cast).orElse(null);
        if (task != null) {
            if (document.getNextRevision() != null) {
                task.setNextDeadline(document.getNextRevision());
            } else {
                task.setNextDeadline(LocalDate.of(2099, 1,1));
            }
            setTaskRevisionInterval(document, task);
        }
    }

    @Transactional
    public void createAssociatedCheck(final Document document) {
        if (document.getNextRevision() == null) {
            return;
        }
        final Task task = new Task();
        task.setTaskType(TaskType.CHECK);
        task.setName("Revision af " + document.getName());
        task.setResponsibleUser(document.getResponsibleUser());
        task.setCreatedAt(LocalDateTime.now());
        task.setNextDeadline(document.getNextRevision());
        task.setNotifyResponsible(false);
        task.setResponsibleUser(document.getResponsibleUser() != null ? document.getResponsibleUser() : userService.currentUser());
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

    private static void setTaskRevisionInterval(final Document document, final Task task) {
        switch(document.getRevisionInterval()) {
            case YEARLY -> task.setRepetition(TaskRepetition.YEARLY);
            case HALF_YEARLY -> task.setRepetition(TaskRepetition.HALF_YEARLY);
            case EVERY_SECOND_YEAR -> task.setRepetition(TaskRepetition.EVERY_SECOND_YEAR);
            case EVERY_THIRD_YEAR -> task.setRepetition(TaskRepetition.EVERY_THIRD_YEAR);
            case NONE -> task.setRepetition(TaskRepetition.NONE);
        }
    }

}

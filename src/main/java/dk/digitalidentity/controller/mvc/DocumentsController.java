package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.DocumentRevisionInterval;
import dk.digitalidentity.model.entity.enums.DocumentStatus;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.security.annotations.crud.RequireCreateOwnerOnly;
import dk.digitalidentity.security.annotations.crud.RequireDeleteOwnerOnly;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.crud.RequireUpdateOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireDocument;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.ChoiceValueService;
import dk.digitalidentity.service.DocumentService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static dk.digitalidentity.Constants.ASSOCIATED_DOCUMENT_PROPERTY;

@Slf4j
@Controller
@RequireDocument
@RequestMapping("documents")
@RequiredArgsConstructor
public class DocumentsController {
    private final DocumentService documentService;
    private final RelationService relationService;
    private final TaskService taskService;
	private final ChoiceService choiceService;
	private final ChoiceValueService choiceValueService;

	private record DocumentFormDTO(
			@NotEmpty
			String name,

			String description,

			@NotNull
			Long documentTypeId,

			String documentVersion,

			@NotNull
			DocumentStatus status,

			String link,

			@NotNull
			DocumentRevisionInterval revisionInterval,

			@DateTimeFormat(pattern = "dd/MM-yyyy")
			LocalDate nextRevision,

			@NotNull
			User responsibleUser
	) {}

	@RequireReadOwnerOnly
    @GetMapping
    public String documentsList(final Model model) {
		model.addAttribute("document", new DocumentFormDTO(null, null, null, null, null, null, null, null, null));
        model.addAttribute("isSuperuser", SecurityUtil.isOperationAllowed(Roles.UPDATE_OWNER_ONLY));
		model.addAttribute("possibleDocumentTypes", choiceService.findChoiceValuesForListIdentifier("document-type"));
        return "documents/index";
    }

	@Transactional
	@RequireCreateOwnerOnly
	@PostMapping("create")
	public String formCreate(@Valid @ModelAttribute("documentForm") final DocumentFormDTO documentForm,
			@RequestParam(name = "relations", required = false) final Set<Long> relations,
			@RequestParam(name = "includeInYearWheel", required = false, defaultValue = "false") final Boolean includeInYearWheel) {

		final ChoiceValue documentType = choiceValueService.findById(documentForm.documentTypeId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document type"));

		final Document document = new Document();
		document.setName(documentForm.name());
		document.setDescription(documentForm.description());
		document.setDocumentType(documentType);
		document.setDocumentVersion(documentForm.documentVersion());
		document.setStatus(documentForm.status());
		document.setLink(documentForm.link());
		document.setRevisionInterval(documentForm.revisionInterval());
		document.setNextRevision(documentForm.nextRevision());
		document.setResponsibleUser(documentForm.responsibleUser());

		final Document savedDocument = documentService.create(document);
		relationService.setRelationsAbsolute(savedDocument, relations);
		documentService.createAssociatedCheck(document, includeInYearWheel);
		return "redirect:/documents/" + savedDocument.getId();
	}

	public record DocumentEditFormDTO(
			@NotNull
			Long id,

			@NotEmpty
			String name,

			String description,

			@NotNull
			Long documentTypeId,

			String documentVersion,

			@NotNull
			DocumentStatus status,

			String link,

			@NotNull
			DocumentRevisionInterval revisionInterval,

			@DateTimeFormat(pattern = "dd/MM-yyyy")
			LocalDate nextRevision,

			@NotNull
			User responsibleUser
	) {}
	@RequireReadOwnerOnly
	@GetMapping("{id}")
	public String documentView(final Model model, @PathVariable final long id) {
		final Document document = documentService.get(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		if (document.getDocumentType() == null) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Document has no document type");
		}

		DocumentEditFormDTO editForm = new DocumentEditFormDTO(
				document.getId(),
				document.getName(),
				document.getDescription(),
				document.getDocumentType().getId(),
				document.getDocumentVersion(),
				document.getStatus(),
				document.getLink(),
				document.getRevisionInterval(),
				document.getNextRevision(),
				document.getResponsibleUser()
		);

		model.addAttribute("document", document);
		model.addAttribute("documentEditForm", editForm);
		model.addAttribute("changeableDocument", (SecurityUtil.isOperationAllowed(Roles.UPDATE_ALL) || documentService.isResponsibleFor(document)));
		model.addAttribute("responsibleFieldChangeable", !documentService.isResponsibleFor(document));
		model.addAttribute("relations", relationService.findRelationsAsListDTO(document, false));
		final List<Relatable> relatedTasks = relationService.findAllRelatedTo(document);
		final Task task = relatedTasks.stream()
				.filter(r -> r.getRelationType() == RelationType.TASK && r.getProperties().stream()
						.anyMatch(p -> ASSOCIATED_DOCUMENT_PROPERTY.equals(p.getKey()))
				).findFirst().map(Task.class::cast).orElse(null);
		model.addAttribute("includeInYearWheel", task != null ? task.getIncludeInReport() : false);
		model.addAttribute("possibleDocumentTypes", choiceService.findChoiceValuesForListIdentifier("document-type"));
		return "documents/view";
	}

	@RequireUpdateOwnerOnly
	@Transactional
	@PostMapping("edit")
	public String formEdit(@Valid @ModelAttribute("documentEditForm") final DocumentEditFormDTO documentEditForm,
			@RequestParam(name = "includeInYearWheel", required = false, defaultValue = "false") final Boolean includeInYearWheel) {
		final Document existingDocument = documentService.get(documentEditForm.id())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		if(!documentService.isResponsibleFor(existingDocument) && !SecurityUtil.isAdministrator()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

		final ChoiceValue documentType = choiceValueService.findById(documentEditForm.documentTypeId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document type"));

		existingDocument.setName(documentEditForm.name());
		existingDocument.setDocumentType(documentType);
		existingDocument.setDescription(documentEditForm.description());
		existingDocument.setStatus(documentEditForm.status());
		existingDocument.setLink(documentEditForm.link());
		existingDocument.setRevisionInterval(documentEditForm.revisionInterval());
		existingDocument.setNextRevision(documentEditForm.nextRevision());
		existingDocument.setResponsibleUser(documentEditForm.responsibleUser());
		existingDocument.setDocumentVersion(documentEditForm.documentVersion());

		documentService.update(existingDocument, includeInYearWheel);
		documentService.updateAssociatedCheck(existingDocument, includeInYearWheel);

		return "redirect:/documents/" + existingDocument.getId();
	}

    @RequireDeleteOwnerOnly
    @DeleteMapping("{id}")
    @ResponseStatus(value = HttpStatus.OK)
    @Transactional
    public void documentDelete(@PathVariable final Long id) {
        final Document document = documentService.get(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // All related checks should be deleted along with the document
        final List<Task> tasks = taskService.findRelatedTasks(document, t -> t.getTaskType() == TaskType.CHECK);
        taskService.deleteAll(tasks);
        documentService.deleteById(id);
    }
}

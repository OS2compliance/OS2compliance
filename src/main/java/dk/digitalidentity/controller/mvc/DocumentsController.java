package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.dto.DocumentEditFormDTO;
import dk.digitalidentity.model.dto.DocumentFormDTO;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Task;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;


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

	@RequireReadOwnerOnly
    @GetMapping
    public String documentsList(final Model model) {
		model.addAttribute("document", new DocumentFormDTO(null, null, null, null, null, null, null, null, null, List.of(), false));
        model.addAttribute("isSuperuser", SecurityUtil.isOperationAllowed(Roles.UPDATE_OWNER_ONLY));
		model.addAttribute("possibleDocumentTypes", choiceService.findChoiceValuesForListIdentifier("document-type"));
        return "documents/index";
    }

	@Transactional
	@RequireCreateOwnerOnly
	@PostMapping("create")
	public String formCreate(@Valid @ModelAttribute("documentForm") final DocumentFormDTO documentForm,
			@RequestParam(name = "relations", required = false) final Set<Long> relations) {

		final ChoiceValue documentType = choiceValueService.findById(documentForm.getDocumentTypeId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document type"));

		final Document document = new Document();
		document.setName(documentForm.getName());
		document.setDescription(documentForm.getDescription());
		document.setDocumentType(documentType);
		document.setDocumentVersion(documentForm.getDocumentVersion());
		document.setStatus(documentForm.getStatus());
		document.setLink(documentForm.getLink());
		document.setRevisionInterval(documentForm.getRevisionInterval());
		document.setNextRevision(documentForm.getNextRevision());
		document.setResponsibleUser(documentForm.getResponsibleUser());
		document.setTags(new HashSet<>(documentForm.getTags()));
		document.setIncludeInYearWheel(documentForm.isIncludeInYearWheel());


		final Document savedDocument = documentService.create(document);
		relationService.setRelationsAbsolute(savedDocument, relations);
		documentService.createAssociatedCheck(document, documentForm.isIncludeInYearWheel());
		return "redirect:/documents/" + savedDocument.getId();
	}

	@RequireReadOwnerOnly
	@GetMapping("{id}")
	public String documentView(final Model model, @PathVariable final long id) {
		final Document document = documentService.get(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		if (document.getDocumentType() == null) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Document has no document type");
		}
		Task task = documentService.findRelatedCheckTask(document, relationService);

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
				document.getResponsibleUser(),
				document.isIncludeInYearWheel()
		);

		model.addAttribute("document", document);
		model.addAttribute("documentEditForm", editForm);
		model.addAttribute("changeableDocument", (SecurityUtil.isOperationAllowed(Roles.UPDATE_ALL) || documentService.isResponsibleFor(document)));
		model.addAttribute("responsibleFieldChangeable", !documentService.isResponsibleFor(document));
		model.addAttribute("relations", relationService.findRelationsAsListDTO(document, false));
		model.addAttribute("possibleDocumentTypes", choiceService.findChoiceValuesForListIdentifier("document-type"));
		return "documents/view";
	}

	@RequireUpdateOwnerOnly
	@Transactional
	@PostMapping("edit")
	public String formEdit(@Valid @ModelAttribute("documentEditForm") final DocumentEditFormDTO documentEditForm) {
		final Document existingDocument = documentService.get(documentEditForm.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		if(!documentService.isResponsibleFor(existingDocument) && !SecurityUtil.isAdministrator()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

		final ChoiceValue documentType = choiceValueService.findById(documentEditForm.getDocumentTypeId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document type"));

		existingDocument.setName(documentEditForm.getName());
		existingDocument.setDocumentType(documentType);
		existingDocument.setDescription(documentEditForm.getDescription());
		existingDocument.setStatus(documentEditForm.getStatus());
		existingDocument.setLink(documentEditForm.getLink());
		existingDocument.setRevisionInterval(documentEditForm.getRevisionInterval());
		existingDocument.setNextRevision(documentEditForm.getNextRevision());
		existingDocument.setResponsibleUser(documentEditForm.getResponsibleUser());
		existingDocument.setDocumentVersion(documentEditForm.getDocumentVersion());
		existingDocument.setIncludeInYearWheel(documentEditForm.isIncludeInYearWheel());

		Task task = documentService.findRelatedCheckTask(existingDocument, relationService);

		documentService.update(existingDocument, documentEditForm.isIncludeInYearWheel());
		// The document did not have a revision task previously, so we now create one as there is the data for it
		if (task == null && existingDocument.getNextRevision() != null && existingDocument.getRevisionInterval() != null) {
			documentService.createAssociatedCheck(existingDocument, documentEditForm.isIncludeInYearWheel());
		}

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

package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.security.RequireSuperuser;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.service.DocumentService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.util.List;
import java.util.Set;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Slf4j
@Controller
@RequireUser
@RequestMapping("documents")
@RequiredArgsConstructor
public class DocumentsController {
    private final DocumentService documentService;
    private final RelationService relationService;
    private final TaskService taskService;

    @GetMapping
    public String documentsList(final Model model) {
        model.addAttribute("document", new Document());
        return "documents/index";
    }

    @Transactional
    @RequireSuperuser
    @PostMapping("create")
    public String formCreate(@Valid @ModelAttribute final Document document,
            @RequestParam(name = "relations", required = false) final Set<Long> relations) {
        final Document savedDocument = documentService.create(document);
        relationService.setRelationsAbsolute(savedDocument, relations);
        // this will add a relation so make sure to call this after setRelationsAbsolute
        documentService.createAssociatedCheck(document);
        return "redirect:/documents/" + savedDocument.getId();
    }

    @GetMapping("{id}")
    public String documentView(final Model model, @PathVariable final long id) {
        final Document document = documentService.get(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        model.addAttribute("document", document);
        model.addAttribute("relations", relationService.findRelationsAsListDTO(document, false));
        return "documents/view";
    }

    @RequireSuperuser
    @DeleteMapping("{id}")
    @ResponseStatus(value = HttpStatus.OK)
    @Transactional
    public void documentDelete(@PathVariable final Long id) {
        final Document document = documentService.get(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // All related checks should be deleted along with the document
        final List<Task> tasks = taskService.findRelatedTasks(document, t -> t.getTaskType() == TaskType.CHECK);
        taskService.deleteAll(tasks);
        relationService.deleteRelatedTo(id);
        documentService.deleteById(id);
    }

    @Transactional
    @PostMapping("edit")
    public String formEdit(@ModelAttribute final Document document) {
        final Document excistingDocument = documentService.get(document.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !excistingDocument.getResponsibleUser().getUuid().equals(authentication.getPrincipal().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
//        if (document.getNextRevision() != null && document.getNextRevision().isBefore(LocalDate.now())) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der skal vælges en gyldig revideringsdato");
//        }
        excistingDocument.setName(document.getName());
        excistingDocument.setDocumentType(document.getDocumentType());
        excistingDocument.setDescription(document.getDescription());
        excistingDocument.setStatus(document.getStatus());
        excistingDocument.setLink(document.getLink());
        excistingDocument.setRevisionInterval(document.getRevisionInterval());
        excistingDocument.setNextRevision(document.getNextRevision());
        excistingDocument.setResponsibleUser(document.getResponsibleUser());
        excistingDocument.setDocumentVersion(document.getDocumentVersion());

        documentService.update(excistingDocument);
        documentService.updateAssociatedCheck(excistingDocument);

        return "redirect:/documents/" + excistingDocument.getId();
    }
}

package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.TaskDao;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.DocumentService;
import dk.digitalidentity.service.RelationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Slf4j
@Controller
@RequireUser
@RequestMapping("documents")
public class DocumentsController {
    @Autowired
    private DocumentService documentService;
    @Autowired
    private RelationService relationService;
    @Autowired
    private TaskDao taskDao;

    @GetMapping
    public String documentsList(final Model model) {
        model.addAttribute("document", new Document());
        return "documents/index";
    }

    @Transactional
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

    @DeleteMapping("{id}")
    @ResponseStatus(value = HttpStatus.OK)
    @Transactional
    public void documentDelete(@PathVariable final String id) {
        final Long lid = Long.valueOf(id);
        
        // Find all tasks(type: CHECK) related to this Document
        final List<Relation> relations = relationService.findRelatedToWithType( Arrays.asList(lid), RelationType.TASK);
        List<Task> tasks = relations.stream()
            .map(r -> r.getRelationAType().equals(RelationType.TASK) ? r.getRelationAId() : r.getRelationBId())
            .map(taskDao::findById)
            .filter(Optional::isPresent).filter(t -> t.get().getTaskType() == TaskType.CHECK).map(t -> t.get()).collect(Collectors.toList());
        // Delete those tasks
        taskDao.deleteAll(tasks);

        relationService.deleteRelatedTo(lid);
        documentService.deleteById(lid);
    }

    @Transactional
    @PostMapping("edit")
    public String formEdit(@ModelAttribute final Document document) {
        final Document excistingDocument = documentService.get(document.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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

package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.event.IncidentFieldsUpdatedEvent;
import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.IncidentField;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.security.annotations.crud.RequireCreateAll;
import dk.digitalidentity.security.annotations.crud.RequireReadAll;
import dk.digitalidentity.security.annotations.crud.RequireUpdateAll;
import dk.digitalidentity.security.annotations.sections.RequireConfiguration;
import dk.digitalidentity.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Controller
@RequestMapping("incidents")
@RequireConfiguration
@RequiredArgsConstructor
public class IncidentController {
    private final IncidentService incidentService;
    private final ApplicationEventPublisher eventPublisher;

	@RequireReadAll
    @GetMapping("logs")
    public String incidentLog(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("superuser", authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPER_USER)));
        return "incidents/logs/index";
    }

    @RequireReadAll
    @GetMapping("questions")
    public String incidentQuestions() {
        return "incidents/questions/index";
    }

    @RequireCreateAll
    @GetMapping("questionForm")
    public String questionForm(final Model model, @RequestParam(name = "id", required = false) Long questionId) {
        if (questionId != null) {
            model.addAttribute("formTitle", "Rediger spørgsmål");
            model.addAttribute("formId", "editForm");
            model.addAttribute("field", incidentService.findField(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        } else {
            model.addAttribute("formTitle", "Nyt spørgsmål");
            model.addAttribute("formId", "createForm");
            model.addAttribute("field", new IncidentField());
        }
        return "incidents/questions/form";
    }

    @RequireUpdateAll
    @PostMapping("questionForm")
    public String questionForm(@Valid @ModelAttribute final IncidentField form) {
        if (form.getId() != null) {
            final IncidentField toUpdate = incidentService.findField(form.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            toUpdate.setQuestion(form.getQuestion());
            toUpdate.setIncidentType(form.getIncidentType());
            toUpdate.setIndexColumnName(form.getIndexColumnName());
            toUpdate.setDefinedList(form.getDefinedList());
        } else {
            form.setSortKey(incidentService.nextIncidentFieldSortKey());
            incidentService.save(form);
        }
        eventPublisher.publishEvent(new IncidentFieldsUpdatedEvent());
        return "redirect:/incidents/questions";
    }

	@RequireCreateAll
    @GetMapping("logForm")
    public String logForm(final Model model, @RequestParam(name = "id", required = false) final Long incidentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("superuser", authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPER_USER)));
        if (incidentId != null) {
            final Incident incident = incidentService.findById(incidentId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            model.addAttribute("formTitle", "Rediger hændelse");
            model.addAttribute("formId", "editForm");
            model.addAttribute("incident", incident);
            // The incident responses only contains identifiers for the objects it points to, so we need to look up
            // the object to be able to show the names in the edit dialog.
            model.addAttribute("responseEntities", incidentService.lookupResponseEntities(incident));
            model.addAttribute("responseUsers", incidentService.lookupResponseUsers(incident));
            model.addAttribute("responseOrganisations", incidentService.lookupResponseOrganisations(incident));
        } else {
            final Incident incident = new Incident();
            incidentService.addDefaultFieldResponses(incident);
            model.addAttribute("formTitle", "Ny hændelse");
            model.addAttribute("formId", "createForm");
            model.addAttribute("incident", incident);
        }
        return "incidents/logs/form";
    }

@RequireReadAll
    @GetMapping("logs/{id}")
    public String viewIncident(final Model model, @PathVariable final Long id) {
        final Incident incident = incidentService.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("incident", incident);
        // The incident responses only contains identifiers for the objects it points to, so we need to look up
        // the object to be able to show the names in the edit dialog.
        model.addAttribute("changeableIncident", (authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPER_USER)) || (incident.getCreator() != null && SecurityUtil.getPrincipalUuid().equals(incident.getCreator().getUuid()))));
        model.addAttribute("responseEntities", incidentService.lookupResponseEntities(incident));
        model.addAttribute("responseUsers", incidentService.lookupResponseUsers(incident));
        model.addAttribute("responseOrganisations", incidentService.lookupResponseOrganisations(incident));
        model.addAttribute("formId", "view");
        return "incidents/logs/view";
    }

	@RequireCreateAll
    @PostMapping("log")
    public String createOrUpdateIncident(@ModelAttribute final Incident incident) {
        if (incident.getId() != null) {
            final Incident existingIncident = incidentService.findById(incident.getId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            existingIncident.setName(incident.getName());
            existingIncident.getResponses().clear();
            existingIncident.getResponses().addAll(incident.getResponses());
            existingIncident.getResponses()
                .forEach(r -> r.setIncident(existingIncident));
            incidentService.ensureRelations(incident);
            return "redirect:/incidents/logs/" + incident.getId();
        } else {
            incident.getResponses()
                .forEach(r -> r.setIncident(incident));
            final Incident saved = incidentService.save(incident);
            incidentService.ensureRelations(saved);
            return "redirect:/incidents/logs/" + saved.getId();
        }
    }
}

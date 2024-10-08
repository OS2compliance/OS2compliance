package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.entity.Incident;
import dk.digitalidentity.model.entity.IncidentField;
import dk.digitalidentity.security.RequireAdminstrator;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Controller
@RequestMapping("incidents")
@RequireUser
@RequiredArgsConstructor
public class IncidentController {
    private final IncidentService incidentService;

    @GetMapping("logs")
    public String incidentLog(final Model model) {
        return "incidents/logs/index";
    }

    @RequireAdminstrator
    @GetMapping("questions")
    public String incidentQuestions(final Model model) {
        return "incidents/questions/index";
    }

    @RequireAdminstrator
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

    @RequireAdminstrator
    @PostMapping("questionForm")
    public String questionForm(@Valid @ModelAttribute final IncidentField form) {
        if (form.getId() != null) {
            final IncidentField toUpdate = incidentService.findField(form.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            toUpdate.setQuestion(form.getQuestion());
            toUpdate.setIncidentType(form.getIncidentType());
            toUpdate.setIndexColumn(form.isIndexColumn());
            toUpdate.setDefinedList(form.getDefinedList());
        } else {
            form.setSortKey(incidentService.nextIncidentFieldSortKey());
            incidentService.save(form);
        }
        return "redirect:/incidents/questions";
    }

    @GetMapping("logForm")
    public String logForm(final Model model, @RequestParam(name = "id", required = false) final Long logId) {
        if (logId != null) {
            model.addAttribute("formTitle", "Rediger spørgsmål");
            model.addAttribute("formId", "editForm");
            // TODO
//            model.addAttribute("field", incidentService.findField(questionId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        } else {
            final Incident incident = new Incident();
            incidentService.addDefaultFieldResponses(incident);
            model.addAttribute("formTitle", "Nyt spørgsmål");
            model.addAttribute("formId", "createForm");
            model.addAttribute("incident", incident);
        }
        return "incidents/logs/form";
    }
}

package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.entity.IncidentField;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    @GetMapping("questions")
    public String incidentQuestions(final Model model) {
        return "incidents/questions/index";
    }

    @GetMapping("questionForm")
    public String questionForm(final Model model, @RequestParam(name = "id", required = false) Long questionId) {
        if (questionId != null) {
            model.addAttribute("formTitle", "Rediger spørgsmål");
            model.addAttribute("formId", "editForm");
            model.addAttribute("field", incidentService.findField(questionId));
        } else {
            model.addAttribute("formTitle", "Nyt spørgsmål");
            model.addAttribute("formId", "createForm");
            model.addAttribute("field", new IncidentField());
        }
        return "incidents/questions/form";
    }
}

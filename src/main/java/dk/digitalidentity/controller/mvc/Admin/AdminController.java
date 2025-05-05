package dk.digitalidentity.controller.mvc.Admin;

import dk.digitalidentity.model.dto.EmailTemplateDTO;
import dk.digitalidentity.model.entity.EmailTemplate;
import dk.digitalidentity.security.RequireAdministrator;
import dk.digitalidentity.service.EmailTemplateService;
import dk.digitalidentity.service.ResponsibleUserViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequireAdministrator
@RequestMapping("admin")
@RequiredArgsConstructor
public class AdminController {
    private final ResponsibleUserViewService responsibleUserViewService;
    private final EmailTemplateService emailTemplateService;


    @GetMapping("inactive")
    public String inactiveResponsibleList(Model model) {
        model.addAttribute("users", responsibleUserViewService.findInactiveResponsibleUsers());
        return "admin/inactive_users";
    }

    @GetMapping("mailtemplates")
    public String mailTemplates(final Model model) {
        List<EmailTemplate> templates = emailTemplateService.findAll();
        List<EmailTemplateDTO> templateDTOs = templates.stream()
            .map(t -> EmailTemplateDTO.builder()
                .id(t.getId())
                .message(t.getMessage())
                .title(t.getTitle())
                .templateTypeText(t.getTemplateType().getMessage())
                .enabled(t.isEnabled())
                .emailTemplatePlaceholders(t.getTemplateType().getEmailTemplatePlaceholders())
                .build())
            .collect(Collectors.toList());

        model.addAttribute("templates", templateDTOs);

        return "admin/mailtemplates";
    }


}

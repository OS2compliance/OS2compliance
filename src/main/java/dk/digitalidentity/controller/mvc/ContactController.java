package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.ContactDao;
import dk.digitalidentity.model.entity.Contact;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.security.annotations.RequireSuperuserOrAdministrator;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.RelatableService;
import dk.digitalidentity.service.RelationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequireUser
@RequestMapping("contacts")
@RequiredArgsConstructor
public class ContactController {
    private final RelatableService relatableService;
    private final RelationService relationService;
    private final ContactDao contactDao;
    private final HttpServletRequest httpServletRequest;

    @GetMapping("form")
    public String form(final Model model, @RequestParam(name = "id", required = false) final String id,
                       @RequestParam(name = "sourceRelationId", required = false) final String sourceRelationId,
                       @RequestParam(name = "sourceRelationType", required = false) final String sourceRelationType) {
        if (id == null) {
            final Contact contact = new Contact();
            model.addAttribute("contact", contact);
            model.addAttribute("sourceRelationId", sourceRelationId);
            model.addAttribute("sourceRelationType", sourceRelationType);
        }
        return "contacts/form";
    }

    @RequireSuperuserOrAdministrator
    @Transactional
    @PostMapping("form")
    public String formPost(@ModelAttribute final Contact contact,
                           @RequestParam(name = "sourceRelationId", required = false) final String sourceRelationId,
                           @RequestParam(name = "sourceRelationType", required = false) final String sourceRelationType) {
        final Contact savedContact = contactDao.save(contact);
        final Relatable relationA = relatableService.findById(Long.valueOf(sourceRelationId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        relationService.addRelation(relationA, savedContact);
        return "redirect:" + httpServletRequest.getHeader("Referer");
    }

}

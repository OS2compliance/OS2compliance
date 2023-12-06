package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.ContactDao;
import dk.digitalidentity.dao.RelatableDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.model.entity.Contact;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.security.RequireUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
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

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Controller
@RequireUser
@RequestMapping("contacts")
public class ContactController {
    @Autowired
    private RelatableDao relatableDao;
    @Autowired
    private ContactDao contactDao;
    @Autowired
    private RelationDao relationDao;
    @Autowired
    private HttpServletRequest httpServletRequest;

    @GetMapping("form")
    public String form(final Model model, @RequestParam(name = "id", required = false) final String id,
                       @RequestParam(name = "sourceRelationId", required = false) final String sourceRelationId,
                       @RequestParam(name = "sourceRelationType", required = false) final String sourceRelationType) {
        if (id == null) {
            final Contact contact = new Contact();
            model.addAttribute("contact", contact);
            model.addAttribute("sourceRelationId", sourceRelationId);
            model.addAttribute("sourceRelationType", sourceRelationType);
        } else {
            // TODO Edit
        }
        return "contacts/form";
    }

    @Transactional
    @PostMapping("form")
    public String formPost(@ModelAttribute final Contact contact,
                           @RequestParam(name = "sourceRelationId", required = false) final String sourceRelationId,
                           @RequestParam(name = "sourceRelationType", required = false) final String sourceRelationType) {
        final Contact savedContact = contactDao.save(contact);
        final Relatable relationA = relatableDao.findById(Long.valueOf(sourceRelationId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        relationDao.save(Relation.builder()
                        .relationAId(relationA.getId())
                        .relationAType(RelationType.valueOf(sourceRelationType))
                        .relationBId(savedContact.getId())
                        .relationBType(RelationType.CONTACT)
                .build());
        return "redirect:" + httpServletRequest.getHeader("Referer");
    }

}

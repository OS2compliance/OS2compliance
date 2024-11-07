package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.model.entity.enums.AssetStatus;
import dk.digitalidentity.model.entity.enums.Criticality;
import dk.digitalidentity.model.entity.enums.DataProcessingAgreementStatus;
import dk.digitalidentity.security.RequireAdminstrator;
import dk.digitalidentity.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Controller
@RequestMapping("admin/tags")
@RequireAdminstrator
@RequiredArgsConstructor
public class TagsController {
    private final TagService tagService;

    /**
     * Main endpoint for Tags view
     * @param model
     * @return
     */
    @GetMapping()
    public String tagAdmin(final Model model){
        model.addAttribute("tag", new Tag());
        model.addAttribute("tags",tagService.findAll());
        return "tags/tags_view";
    }

    /**
     * Creates a new tag and redirects to the main tag page
     * @param tag
     * @return redirect to main tags view
     */
    @Transactional
    @PostMapping("create")
    public String createTag(@ModelAttribute final Tag tag) {

        final Tag newTag = tagService.create(tag);
        return "redirect:/admin/tags";
    }

}

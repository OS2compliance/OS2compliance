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

    @GetMapping()
    public String tagAdmin(final Model model){
        model.addAttribute("tag", new Tag());
        model.addAttribute("tags",tagService.findAll());
        return "tags/tags_view";
    }

    @Transactional
    @PostMapping("create")
    public String createTag(@ModelAttribute final Tag tag) {

        final Tag newTag = tagService.create(tag);
        return "redirect:/admin/tags";
    }

}

package dk.digitalidentity.controller.mvc.Admin;

import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.security.annotations.sections.RequireAdmin;
import dk.digitalidentity.service.tag.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("admin/tags")
@RequireAdmin
@RequiredArgsConstructor
public class TagsController {
    private final TagService tagService;

	public record ColorDTO(String label, String colorCode, String contrastCode) {}
	public record TagListDTO(Long id, String title, ColorDTO color, boolean yearWheel) {}
    /**
     * Main endpoint for Tags view
     * @param model
     * @return
     */
    @GetMapping()
    public String tagAdmin(final Model model){
        model.addAttribute("tag", new Tag());
        model.addAttribute("tags",tagService.findAll().stream()
				.map(t -> new TagListDTO(
						t.getId(),
						t.getValue(),
						new ColorDTO(
								t.getColor().getMessage(),
								t.getColor().getHexCode(),
								t.getColor().getContrastHexCode()),
								t.isYearWheel()
				))
				.toList());
        return "tags/tags_view";
    }

	@GetMapping({"{id}"})
	public String getTag(@PathVariable Long id, Model model){
		Tag tag = tagService.findById(id)
				.orElseThrow();

		model.addAttribute("tag", tag);
		return "tags/fragment/edit_tag_modal";
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

	/**
	 * Updates an existing tag and redirects to the main tag page
	 * @param tag
	 * @return redirect to main tags view
	 */
	@Transactional
	@PostMapping("update")
	public String updateTag(@ModelAttribute final Tag tag) {
		Tag existingTag = tagService.getByID(tag.getId())
				.orElseThrow();

		tagService.update(existingTag, tag);

		return "redirect:/admin/tags";
	}
}

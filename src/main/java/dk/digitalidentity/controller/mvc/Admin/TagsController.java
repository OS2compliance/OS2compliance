package dk.digitalidentity.controller.mvc.Admin;

import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.security.annotations.sections.RequireAdmin;
import dk.digitalidentity.service.TagService;
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
	public record TagListDTO(Long id, String title, ColorDTO color) {}
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
								t.getColor().getContrastHexCode())
				))
				.toList());
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

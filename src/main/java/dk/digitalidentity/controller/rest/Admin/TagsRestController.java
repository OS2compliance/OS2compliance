package dk.digitalidentity.controller.rest.Admin;

import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.security.annotations.crud.RequireDeleteAll;
import dk.digitalidentity.security.annotations.crud.RequireUpdateAll;
import dk.digitalidentity.security.annotations.sections.RequireAdmin;
import dk.digitalidentity.service.tag.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequireAdmin
@RequiredArgsConstructor
@RequestMapping( "rest/tags")
public class TagsRestController {
    private final TagService tagService;

    /**
     * Deletes the Tag with the given ID, if it exists
     * @param tagId
     * @return
     */
	@RequireDeleteAll
    @Transactional
    @DeleteMapping(value = "{tagId}/delete")
    public ResponseEntity<?> delete(@PathVariable("tagId") final Long tagId) {
        final Tag tag = tagService.getByID(tagId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        tagService.delete(tag);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

	@RequireUpdateAll
	@PostMapping("add/{targetType}/{targetId}")
	public ResponseEntity<Void> addTag(@PathVariable final Long targetId, @PathVariable final String targetType, @RequestBody final List<Long> tags) {

		if (tags == null || targetId == null || targetType == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}

		for (final Long tagId : tags) {
			if (tagId != null) {
				tagService.addTag(tagId, targetType, targetId);
			}
		}

		return ResponseEntity.ok().build();
	}

	@RequireDeleteAll
	@DeleteMapping("{tagId}/remove/{targetType}/{targetId}")
	public ResponseEntity<Void> removeTag(@PathVariable final Long tagId, @PathVariable final Long targetId, @PathVariable final String targetType) {

		if (tagId == null || targetId == null || targetType == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}

		tagService.removeTag(tagId, targetType, targetId);
		return ResponseEntity.ok().build();
	}

}

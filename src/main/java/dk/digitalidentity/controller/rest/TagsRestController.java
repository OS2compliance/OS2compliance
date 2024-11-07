package dk.digitalidentity.controller.rest;

import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.security.RequireAdminstrator;
import dk.digitalidentity.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequireAdminstrator
@RequiredArgsConstructor
@RequestMapping(value = "rest/tags", consumes = "application/json", produces = "application/json")
public class TagsRestController {
    private final TagService tagService;

    @Transactional
    @DeleteMapping(value = "{tagId}")
    public ResponseEntity<?> delete(@PathVariable("tagId") final Long tagId) {
        final Tag tag = tagService.getByID(tagId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        tagService.delete(tag);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}

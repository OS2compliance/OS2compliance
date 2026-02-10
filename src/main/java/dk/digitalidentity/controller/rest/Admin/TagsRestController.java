package dk.digitalidentity.controller.rest.Admin;

import dk.digitalidentity.model.ExcelColumn;
import dk.digitalidentity.model.ExcludeFromExport;
import dk.digitalidentity.model.dto.excel.ExcelExportRequest;
import dk.digitalidentity.model.dto.excel.ExportMetadataDTO;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.security.annotations.crud.RequireDeleteAll;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.crud.RequireUpdateAll;
import dk.digitalidentity.security.annotations.sections.RequireAdmin;
import dk.digitalidentity.service.ExcelExportHelperService;
import dk.digitalidentity.service.tag.TagService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequireAdmin
@RequiredArgsConstructor
@RequestMapping( "rest/tags")
public class TagsRestController {
    private final TagService tagService;
	private final ExcelExportHelperService excelExportHelperService;

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

	record TagExportDTO(
			@ExcludeFromExport
			Long id,
			@ExcelColumn(headerName = "Tag", order = 1)
			String title,
			@ExcelColumn(headerName = "Farve", order = 2)
			String color
	) {}

	@GetMapping("export-metadata")
	public ExportMetadataDTO getExportMetadata() {
		return excelExportHelperService.getMetadata(TagExportDTO.class);
	}

	@PostMapping("export-custom")
	@RequireReadOwnerOnly
	public void exportCustom(
			@RequestBody ExcelExportRequest request,
			HttpServletResponse response
	) throws IOException {
		List<Long> ids = request.getSelectedIds().stream().map(Long::parseLong).toList();

		List<Tag> tags = tagService.findByIds(ids);

		if (tags.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		List<TagExportDTO> dtos = tags.stream()
				.map(tag -> new TagExportDTO(
						tag.getId(),
						tag.getValue(),
						tag.getColor() != null ? tag.getColor().getMessage() : ""
				))
				.toList();

		excelExportHelperService.exportEntities(
				TagExportDTO.class,
				dtos,
				request,
				response
		);
	}

}

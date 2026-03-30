package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.DocumentGridDao;
import dk.digitalidentity.mapping.DocumentMapper;
import dk.digitalidentity.model.dto.DocumentDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.excel.EntityListItemDTO;
import dk.digitalidentity.model.dto.excel.EntityListRequest;
import dk.digitalidentity.model.dto.excel.ExcelExportRequest;
import dk.digitalidentity.model.dto.excel.ExportMetadataDTO;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.DocumentGrid;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireDocument;
import dk.digitalidentity.service.DocumentService;
import dk.digitalidentity.service.ExcelExportHelperService;
import dk.digitalidentity.service.SecurityUserService;
import dk.digitalidentity.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

@Slf4j
@RestController
@RequestMapping("rest/documents")
@RequireDocument
@RequiredArgsConstructor
public class DocumentRestController {
    private final DocumentGridDao documentGridDao;
    private final DocumentMapper mapper;
    private final UserService userService;
	private final ExcelExportHelperService excelExportHelperService;
	private final SecurityUserService securityUserService;
	private final DocumentService documentService;

	@RequireReadOwnerOnly
	@PostMapping("list")
	public PageDTO<DocumentDTO> list(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "order", required = false) String sortColumn,
			@RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
			@RequestParam Map<String, String> filters // Dynamic filters for search fields
	) {
		User user = securityUserService.getCurrentUserOrThrow();

        Page<DocumentGrid> documents = documentService.getDocuments(sortColumn, sortDirection, filters, page, limit, user);

		Set<Long> entityIds = documents.getContent().stream().map(DocumentGrid::getId).collect(Collectors.toSet());
		Map<Long, Tag> tagsById = documentService.findTagsByEntityIds(entityIds).stream()
				.collect(Collectors.toMap(Tag::getId, t -> t, (a, b) -> b));

        assert documents != null;
        return new PageDTO<>(documents.getTotalElements(), mapper.toDTO(documents.getContent(), tagsById));
    }

	@RequireReadOwnerOnly
    @PostMapping("list/{id}")
    public PageDTO<DocumentDTO> list(
        @PathVariable(name = "id") final String uuid,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "limit", defaultValue = "50") int limit,
        @RequestParam(value = "order", required = false) String sortColumn,
        @RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
        @RequestParam Map<String, String> filters // Dynamic filters for search fields
    ) {
        final User user = userService.findByUuid(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		Page<DocumentGrid> documents = documentGridDao.findAllWithAssignedUser(
				validateSearchFilters(filters, DocumentGrid.class),
				user,
				buildPageable(page, limit, sortColumn, sortDirection),
				DocumentGrid.class
		);

		Set<Long> entityIds = documents.getContent().stream().map(DocumentGrid::getId).collect(Collectors.toSet());
		Map<Long, Tag> tagsById = documentService.findTagsByEntityIds(entityIds).stream()
				.collect(Collectors.toMap(Tag::getId, t -> t, (a, b) -> b));

        assert documents != null;
        return new PageDTO<>(documents.getTotalElements(), mapper.toDTO(documents.getContent(), tagsById));
    }

	@GetMapping("export-metadata")
	@RequireReadOwnerOnly
	public ExportMetadataDTO getExportMetadata() {
		return excelExportHelperService.getMetadata(DocumentDTO.class);
	}

	@PostMapping("export-entities")
	@RequireReadOwnerOnly
	public List<EntityListItemDTO> getEntitiesForExport(@RequestBody EntityListRequest request) {
		User user = securityUserService.getCurrentUserOrThrow();

		Page<DocumentGrid> documents = documentService.getDocuments(
				null,
				"ASC",
				request.getFilters(),
				0,
				Integer.MAX_VALUE,
				user
		);

		return excelExportHelperService.toEntityListItems(
				documents.getContent(),
				DocumentGrid::getId,
				DocumentGrid::getName
		);
	}

	@PostMapping("export-custom")
	@RequireReadOwnerOnly
	public void exportCustom(
			@RequestBody ExcelExportRequest request,
			HttpServletResponse response
	) throws IOException {
		User user = securityUserService.getCurrentUserOrThrow();

		List<Long> ids = request.getSelectedIds().stream()
				.map(Long::parseLong)
				.toList();
		List<Document> documents = documentService.findByIds(ids, user);

		excelExportHelperService.exportEntities(
				documents,
				DocumentDTO.class,
				mapper::toDTOForExportFromDocuments,
				request,
				response
		);
	}
}

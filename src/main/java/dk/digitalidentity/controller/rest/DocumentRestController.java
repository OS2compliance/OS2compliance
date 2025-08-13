package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.DocumentGridDao;
import dk.digitalidentity.mapping.DocumentMapper;
import dk.digitalidentity.model.dto.DocumentDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.DocumentGrid;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireDocument;
import dk.digitalidentity.service.ExcelExportService;
import dk.digitalidentity.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
	private final ExcelExportService excelExportService;

	@RequireReadOwnerOnly
    @PostMapping("list")
    public PageDTO<DocumentDTO> list(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "limit", defaultValue = "50") int limit,
        @RequestParam(value = "order", required = false) String sortColumn,
        @RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
			@RequestParam(value = "export", defaultValue = "false") boolean export,
			@RequestParam(value = "fileName", defaultValue = "export.xlsx") String fileName,
        @RequestParam Map<String, String> filters // Dynamic filters for search fields
    ) throws IOException {
		final String userUuid = SecurityUtil.getLoggedInUserUuid();
		final User user = userService.findByUuid(userUuid)
				.orElseThrow();
		if (userUuid == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

		// For export mode, get ALL records (no pagination)
		if (export) {
			Page<DocumentGrid> allDocuments;
			if (SecurityUtil.isOperationAllowed(Roles.READ_ALL)) {
			allDocuments = documentGridDao.findAllWithColumnSearch(
					validateSearchFilters(filters, DocumentGrid.class),
					buildPageable(page, Integer.MAX_VALUE, sortColumn, sortDirection),
					DocumentGrid.class
			);
			} else {
				allDocuments = documentGridDao.findAllWithAssignedUser(
						validateSearchFilters(filters, DocumentGrid.class),
						user,
						buildPageable(page, Integer.MAX_VALUE, sortColumn, sortDirection),
						DocumentGrid.class
				);
			}

			List<DocumentDTO> allData = mapper.toDTO(allDocuments.getContent());
			excelExportService.exportToExcel(allData, fileName, response);
			return null;
		}

        Page<DocumentGrid> documents;
		if (SecurityUtil.isOperationAllowed(Roles.READ_ALL)) {
			// Logged in user can see all
			documents = documentGridDao.findAllWithColumnSearch(
					validateSearchFilters(filters, DocumentGrid.class),
					buildPageable(page, limit, sortColumn, sortDirection),
					DocumentGrid.class
			);
		}
		else {
			// Logged in user can see only own
			documents = documentGridDao.findAllWithAssignedUser(
					validateSearchFilters(filters, DocumentGrid.class),
					user,
					buildPageable(page, limit, sortColumn, sortDirection),
					DocumentGrid.class
			);
		}

        assert documents != null;
        return new PageDTO<>(documents.getTotalElements(), mapper.toDTO(documents.getContent()));
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

        assert documents != null;
        return new PageDTO<>(documents.getTotalElements(), mapper.toDTO(documents.getContent()));
    }

}

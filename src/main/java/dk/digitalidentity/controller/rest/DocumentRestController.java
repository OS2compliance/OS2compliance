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
import dk.digitalidentity.service.UserService;
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

import java.util.Map;

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

@Slf4j
@RestController
@RequestMapping("rest/documents")
@RequireUser
@RequiredArgsConstructor
public class DocumentRestController {
    private final DocumentGridDao documentGridDao;
    private final DocumentMapper mapper;
    private final UserService userService;

    @PostMapping("list")
    public PageDTO<DocumentDTO> list(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "limit", defaultValue = "50") int limit,
        @RequestParam(value = "order", required = false) String sortColumn,
        @RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
        @RequestParam Map<String, String> filters // Dynamic filters for search fields
    ) {
        Page<DocumentGrid> documents =  documentGridDao.findAllWithColumnSearch(
            validateSearchFilters(filters, DocumentGrid.class),
            buildPageable(page, limit, sortColumn, sortDirection),
            DocumentGrid.class
        );

        assert documents != null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return new PageDTO<>(documents.getTotalElements(), mapper.toDTO(documents.getContent(), authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)), SecurityUtil.getPrincipalUuid()));
    }

    @PostMapping("list/{id}")
    public PageDTO<DocumentDTO> list(
        @PathVariable(name = "id") final String uuid,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "limit", defaultValue = "50") int limit,
        @RequestParam(value = "order", required = false) String sortColumn,
        @RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
        @RequestParam Map<String, String> filters // Dynamic filters for search fields
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(!SecurityUtil.isSuperUser() && !uuid.equals(SecurityUtil.getPrincipalUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        final User user = userService.findByUuid(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Page<DocumentGrid> documents = documentGridDao.findAllForResponsibleUser(
            validateSearchFilters(filters, DocumentGrid.class),
            buildPageable(page, limit, sortColumn, sortDirection),
            DocumentGrid.class, user);

        assert documents != null;
        return new PageDTO<>(documents.getTotalElements(), mapper.toDTO(documents.getContent(), authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)), SecurityUtil.getPrincipalUuid()));
    }

}

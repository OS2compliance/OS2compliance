package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.DocumentGridDao;
import dk.digitalidentity.mapping.DocumentMapper;
import dk.digitalidentity.model.dto.DocumentDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.DocumentGrid;
import dk.digitalidentity.security.RequireSuperuser;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

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
            @RequestParam(name = "search", required = false) final String search,
            @RequestParam(name = "page", required = false) final Integer page,
            @RequestParam(name = "size", required = false) final Integer size,
            @RequestParam(name = "order", required = false) final String order,
            @RequestParam(name = "dir", required = false) final String dir) {
        Sort sort = null;
        if (StringUtils.isNotEmpty(order) && containsField(order)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, order);
        } else {
            sort = Sort.by(Sort.Direction.ASC, "name");
        }
        final Pageable sortAndPage = PageRequest.of(page, size, sort);
        Page<DocumentGrid> documents = null;
        if (StringUtils.isNotEmpty(search)) {
            // search and page
            final List<String> searchableProperties = Arrays.asList("name", "responsibleUser.name", "nextRevision", "localizedEnums");
            documents = documentGridDao.findAllCustom(searchableProperties, search, sortAndPage, DocumentGrid.class);
        } else {
            // Fetch paged and sorted
            documents = documentGridDao.findAll(sortAndPage);
        }
        assert documents != null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return new PageDTO<>(documents.getTotalElements(), mapper.toDTO(documents.getContent(), authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)), authentication.getPrincipal().toString()));
    }

    @PostMapping("list/{id}")
    public PageDTO<DocumentDTO> list(
        @PathVariable(name = "id", required = true) final String uuid,
        @RequestParam(name = "search", required = false) final String search,
        @RequestParam(name = "page", required = false) final Integer page,
        @RequestParam(name = "size", required = false) final Integer size,
        @RequestParam(name = "order", required = false) final String order,
        @RequestParam(name = "dir", required = false) final String dir) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !authentication.getPrincipal().equals(uuid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Sort sort = null;
        final User user = userService.findByUuid(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (StringUtils.isNotEmpty(order) && containsField(order)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, order);
        }
        final Pageable sortAndPage = sort != null ?  PageRequest.of(page, size, sort) : PageRequest.of(page, size);
        Page<DocumentGrid> documents = null;
        if (StringUtils.isNotEmpty(search)) {
            // search and page
            final List<String> searchableProperties = Arrays.asList("id", "name", "documentType", "nextRevision", "status", "tags");
            documents = documentGridDao.findAllForResponsibleUser(searchableProperties, search, sortAndPage, DocumentGrid.class, user);
        } else {
            // Fetch paged and sorted
            documents = documentGridDao.findAllByResponsibleUser(user, sortAndPage);
        }
        assert documents != null;
        return new PageDTO<>(documents.getTotalElements(), mapper.toDTO(documents.getContent(), authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)), authentication.getPrincipal().toString()));
    }

    private boolean containsField(final String fieldName) {
        return fieldName.equals("name") || fieldName.equals("documentType") || fieldName.equals("documentTypeOrder") || fieldName.equals("responsibleUser.name")
                || fieldName.equals("nextRevision") || fieldName.equals("status") || fieldName.equals("statusOrder");
    }
}

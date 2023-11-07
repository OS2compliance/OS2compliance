package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.dao.grid.DocumentGridDao;
import dk.digitalidentity.mapping.DocumentMapper;
import dk.digitalidentity.model.dto.DocumentDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.DocumentGrid;
import dk.digitalidentity.security.RequireUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("rest/documents")
@RequireUser
public class DocumentRestController {

    @Autowired
    private DocumentGridDao documentGridDao;
    @Autowired
    private DocumentMapper mapper;
    @Autowired
    private UserDao userDao;

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
        }
        final Pageable sortAndPage = sort != null ?  PageRequest.of(page, size, sort) : PageRequest.of(page, size);
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
        return new PageDTO<>(documents.getTotalElements(), mapper.toDTO(documents.getContent()));
    }

    @PostMapping("list/{id}")
    public PageDTO<DocumentDTO> list(
        @PathVariable(name = "id", required = true) final String uuid,
        @RequestParam(name = "search", required = false) final String search,
        @RequestParam(name = "page", required = false) final Integer page,
        @RequestParam(name = "size", required = false) final Integer size,
        @RequestParam(name = "order", required = false) final String order,
        @RequestParam(name = "dir", required = false) final String dir) {
        Sort sort = null;
        final User user = userDao.findByUuidAndActiveIsTrue(uuid);
        if (StringUtils.isNotEmpty(order) && containsField(order)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, order);
        }
        final Pageable sortAndPage = sort != null ?  PageRequest.of(page, size, sort) : PageRequest.of(page, size);
        Page<DocumentGrid> documents = null;
        if (StringUtils.isNotEmpty(search)) {
            // search and page
            final List<String> searchableProperties = Arrays.asList("id", "name", "documentType", "nextRevision", "status");
            documents = documentGridDao.findAllCustomForResponsibleUser(searchableProperties, search, sortAndPage, DocumentGrid.class, user);
        } else {
            // Fetch paged and sorted
            documents = documentGridDao.findAllByResponsibleUser(user, sortAndPage);
        }
        assert documents != null;
        return new PageDTO<>(documents.getTotalElements(), mapper.toDTO(documents.getContent()));
    }

    private boolean containsField(final String fieldName) {
        return fieldName.equals("name") || fieldName.equals("documentType") || fieldName.equals("responsibleUserId")
                || fieldName.equals("nextRevision") || fieldName.equals("status");
    }
}

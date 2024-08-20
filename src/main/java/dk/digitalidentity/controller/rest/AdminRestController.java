package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.InactiveResponsibleGridDao;
import dk.digitalidentity.mapping.InactiveResponsibleMapper;
import dk.digitalidentity.model.dto.InactiveResponsibleDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.RiskDTO;
import dk.digitalidentity.model.entity.grid.InactiveResponsibleGrid;
import dk.digitalidentity.security.RequireAdminstrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

@Slf4j
@RestController
@RequestMapping("rest/admin")
@RequireAdminstrator
@RequiredArgsConstructor
public class AdminRestController {
    private final InactiveResponsibleGridDao inactiveResponsibleGridDao;
    private final InactiveResponsibleMapper mapper;

    @PostMapping("inactive/list")
    public PageDTO<InactiveResponsibleDTO> list(
        @RequestParam(name = "search", required = false) final String search,
        @RequestParam(name = "page", required = false) final Integer page,
        @RequestParam(name = "size", required = false) final Integer size,
        @RequestParam(name = "order", required = false) final String order,
        @RequestParam(name = "dir", required = false) final String dir
    ) {
        Sort sort = null;
        if (isNotEmpty(order) && containsField(order)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, order);
        }
        final Pageable sortAndPage = sort != null ?  PageRequest.of(page, size, sort) : PageRequest.of(page, size);
        final Page<InactiveResponsibleGrid> risks;
        if (StringUtils.isNotEmpty(search)) {
            final List<String> searchableProperties = Arrays.asList("name", "userId");
            risks = inactiveResponsibleGridDao.findAllCustom(searchableProperties, search, sortAndPage, InactiveResponsibleGrid.class);
        } else {
            // Fetch paged and sorted
            risks = inactiveResponsibleGridDao.findAll(sortAndPage);
        }
        assert risks != null;
        return new PageDTO<>(risks.getTotalElements(), mapper.toDTO(risks.getContent()));
    }

    private boolean containsField(final String fieldName) {
        return fieldName.equals("name") || fieldName.equals("userId");
    }
}

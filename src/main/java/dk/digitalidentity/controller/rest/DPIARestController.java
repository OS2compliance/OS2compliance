package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.DPIAGridDao;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.grid.DPIAGrid;
import dk.digitalidentity.security.RequireSuperuserOrAdministrator;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.DPIAService;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

@Slf4j
@RestController
@RequestMapping("rest/dpia")
@RequireUser
@RequiredArgsConstructor
public class DPIARestController {
    private final DPIAGridDao dpiaGridDao;
    private final DPIAService dpiaService;

    public record DPIADTO(long id, String assetName, LocalDateTime updatedAt, int taskCount) {
    }

    @PostMapping("list")
    public PageDTO<DPIADTO> list(
        @RequestParam(name = "search", required = false) final String search,
        @RequestParam(name = "page", required = false, defaultValue = "0") final Integer page,
        @RequestParam(name = "size", required = false, defaultValue = "50") final Integer size,
        @RequestParam(name = "order", required = false) final String order,
        @RequestParam(name = "dir", required = false) final String dir
    ) {
        Sort sort = null;
        if (isNotEmpty(order)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, order);
        }
        final Pageable sortAndPage = sort != null ? PageRequest.of(page, size, sort) : PageRequest.of(page, size);
        final Page<DPIAGrid> dpiaGrids;
        if (StringUtils.isNotEmpty(search)) {
            final List<String> searchableProperties = Arrays.asList("assetName", "updatedAt");
            dpiaGrids = dpiaGridDao.findAllCustom(searchableProperties, search, sortAndPage, DPIAGrid.class);
        } else {
            // Fetch paged and sorted
            dpiaGrids = dpiaGridDao.findAll(sortAndPage);
        }
        assert dpiaGrids != null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return new PageDTO<>(dpiaGrids.getTotalElements(), dpiaGrids.stream().map(dpia ->
                new DPIADTO(
                    dpia.getId(),
                    dpia.getAssetName(),
                    dpia.getUpdatedAt(),
                    dpia.getTaskCount()
                ))
            .toList());
    }

    @DeleteMapping("delete/{id}")
    @RequireSuperuserOrAdministrator
    @ResponseStatus(value = HttpStatus.OK)
    @Transactional
    public void riskDelete(@PathVariable final Long id) {
        dpiaService.delete(id);
    }
}

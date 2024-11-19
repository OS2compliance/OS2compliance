package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.RegisterGridDao;
import dk.digitalidentity.mapping.RegisterMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.RegisterDTO;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.RegisterGrid;
import dk.digitalidentity.security.RequireSuperuser;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
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
@RequestMapping("rest/registers")
@RequireUser
@RequiredArgsConstructor
public class RegisterRestController {
    private final RegisterGridDao registerGridDao;
    private final RegisterMapper mapper;
    private final UserService userService;

    @PostMapping("list")
    public PageDTO<RegisterDTO> list(
            @RequestParam(name = "search", required = false) final String search,
            @RequestParam(name = "page", required = false, defaultValue = "0") final Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "50") final Integer size,
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
        Page<RegisterGrid> registers = null;
        if (StringUtils.isNotEmpty(search)) {
            // search and page
            final List<String> searchableProperties = Arrays.asList("name", "responsibleOUNames", "responsibleUserNames", "updatedAt", "localizedEnums");
            registers = registerGridDao.findAllCustom(searchableProperties, search, sortAndPage, RegisterGrid.class);
        } else {
            // Fetch paged and sorted
            registers = registerGridDao.findAll(sortAndPage);
        }
        assert registers != null;
        return new PageDTO<>(registers.getTotalElements(), mapper.toDTO(registers.getContent()));
    }

    @PostMapping("list/{id}")
    public PageDTO<RegisterDTO> list(
        @PathVariable(name = "id", required = true) final String uuid,
        @RequestParam(name = "search", required = false) final String search,
        @RequestParam(name = "page", required = false, defaultValue = "0") final Integer page,
        @RequestParam(name = "size", required = false, defaultValue = "50") final Integer size,
        @RequestParam(name = "order", required = false) final String order,
        @RequestParam(name = "dir", required = false) final String dir) {
        if(!SecurityUtil.isSuperUser() && !uuid.equals(SecurityUtil.getPrincipalUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Sort sort = null;
        final User user = userService.findByUuid(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (StringUtils.isNotEmpty(order) && containsField(order)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, order);
        } else {
            sort = Sort.by(Sort.Direction.ASC, "name");
        }
        final Pageable sortAndPage = PageRequest.of(page, size, sort);
        Page<RegisterGrid> registers = null;
        if (StringUtils.isNotEmpty(search)) {
            //search and page
            final List<String> searchableProperties = Arrays.asList("name", "responsibleOUNames", "responsibleUserNames", "updatedAt", "consequence", "risk", "status");
            registers = registerGridDao.findAllForResponsibleUser(searchableProperties, search, sortAndPage, RegisterGrid.class, user);
        } else {
            // Fetch paged and sorted
            registers = registerGridDao.findAllByResponsibleUserUuidsContaining(user.getUuid(), sortAndPage) ;
        }
        assert registers != null;
        return new PageDTO<>(registers.getTotalElements(), mapper.toDTO(registers.getContent()));
    }

    private boolean containsField(final String fieldName) {
        return fieldName.equals("name") || fieldName.equals("responsibleUserNames") || fieldName.equals("responsibleOUNames")
                || fieldName.equals("updatedAt") || fieldName.equals("consequenceOrder") || fieldName.equals("riskOrder") || fieldName.equals("departmentNames") || fieldName.equals("assetAssessmentOrder")
                || fieldName.equals("statusOrder") || fieldName.equals("assetCount");
    }
}

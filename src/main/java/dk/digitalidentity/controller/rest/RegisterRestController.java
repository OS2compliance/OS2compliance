package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.RegisterGridDao;
import dk.digitalidentity.mapping.RegisterMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.RegisterDTO;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.RegisterGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireRegister;
import dk.digitalidentity.service.RegisterService;
import dk.digitalidentity.service.ExcelExportService;
import dk.digitalidentity.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

@Slf4j
@RestController
@RequestMapping("rest/registers")
@RequireRegister
@RequiredArgsConstructor
public class RegisterRestController {
	private final RegisterGridDao registerGridDao;
    private final RegisterMapper mapper;
    private final UserService userService;
	private final RegisterService registerService;
	private final ExcelExportService excelExportService;

	@RequireReadOwnerOnly
    @PostMapping("list")
    public PageDTO<RegisterDTO> list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "order", required = false) String sortColumn,
            @RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
			@RequestParam(value = "export", defaultValue = "false") boolean export,
			@RequestParam(value = "fileName", defaultValue = "export.xlsx") String fileName,
            @RequestParam Map<String, String> filters, // Dynamic filters for search fields
			HttpServletResponse response
    ) throws IOException {
		final String userUuid = SecurityUtil.getLoggedInUserUuid();
		final User user = userService.findByUuid(userUuid)
				.orElseThrow();
		if (userUuid == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

		int pageLimit = limit;
		if(export) {
			// For export mode, get ALL records (no pagination)
			pageLimit = Integer.MAX_VALUE;
		}

		Page<RegisterGrid> registers;
		if (SecurityUtil.isOperationAllowed(Roles.READ_ALL)) {
			// Logged in user can see all
			registers = registerGridDao.findAllWithColumnSearch(
					validateSearchFilters(filters, RegisterGrid.class),
					buildPageable(page, pageLimit, sortColumn, sortDirection),
					RegisterGrid.class
			);
		}
		else {
			// Logged in user can see only own
			registers = registerGridDao.findAllWithAssignedUser(
					validateSearchFilters(filters, RegisterGrid.class),
					user,
					buildPageable(page, pageLimit, sortColumn, sortDirection),
					RegisterGrid.class
			);
		}

		// For export mode, get ALL records (no pagination)
		if (export) {
			List<RegisterDTO> allData = mapper.toDTO(registers.getContent(), registerService);
			excelExportService.exportToExcel(allData, fileName, response);
			return null;
		}

        assert registers != null;
        return new PageDTO<>(registers.getTotalElements(), mapper.toDTO(registers.getContent(), registerService));
    }

	@RequireReadOwnerOnly
    @PostMapping("list/{id}")
    public PageDTO<RegisterDTO> list(
        @PathVariable(name = "id") final String uuid,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "limit", defaultValue = "50") int limit,
        @RequestParam(value = "order", required = false) String sortColumn,
        @RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
        @RequestParam Map<String, String> filters // Dynamic filters for search fields
    ) {
        final User user = userService.findByUuid(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		Page<RegisterGrid> registers = registerGridDao.findAllWithAssignedUser(
				validateSearchFilters(filters, RegisterGrid.class),
				user,
				buildPageable(page, limit, sortColumn, sortDirection),
				RegisterGrid.class
		);

        assert registers != null;
        return new PageDTO<>(registers.getTotalElements(), mapper.toDTO(registers.getContent(), registerService));
    }
}

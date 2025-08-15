package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.SupplierDao;
import dk.digitalidentity.dao.grid.SupplierGridDao;
import dk.digitalidentity.mapping.SupplierMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.SupplierDTO;
import dk.digitalidentity.model.dto.enums.AllowedAction;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.SupplierGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireSupplier;
import dk.digitalidentity.service.UserService;
import dk.digitalidentity.service.ExcelExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;
import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

@SuppressWarnings("ClassEscapesDefinedScope")
@Slf4j
@RestController
@RequestMapping("rest/suppliers")
@RequireSupplier
@RequiredArgsConstructor
public class SupplierRestController {
	private final SupplierGridDao supplierGridDao;
	private final SupplierMapper supplierMapper;
	private final SupplierDao supplierDao;
	private final ExcelExportService excelExportService;
	private final UserService userService;

	record SupplierGridDTO(long id, String name, int solutionCount, String updated, String status, Set<AllowedAction> allowedActions) {}

	@RequireReadOwnerOnly
    @PostMapping("list")
	public PageDTO<SupplierGridDTO> list(
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
		if (userUuid == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		final User user = userService.findByUuid(userUuid)
				.orElseThrow();

		int pageLimit = limit;
		if(export) {
			// For export mode, get ALL records (no pagination)
			pageLimit = Integer.MAX_VALUE;
		}

		Set<AllowedAction> allowedActions = new HashSet<>();
		if (SecurityUtil.isOperationAllowed(Roles.UPDATE_ALL)) {
			allowedActions.add(AllowedAction.UPDATE);
		}
		if (SecurityUtil.isOperationAllowed(Roles.DELETE_ALL)) {
			allowedActions.add(AllowedAction.DELETE);
		}

		Page<SupplierGrid> suppliers;
		if (SecurityUtil.isOperationAllowed(Roles.READ_ALL)) {
			// Logged in user can see all
			suppliers = supplierGridDao.findAllWithColumnSearch(
					validateSearchFilters(filters, SupplierGrid.class),
					buildPageable(page, pageLimit, sortColumn, sortDirection),
					SupplierGrid.class
			);
		}
		else {
			// Logged in user can see only own
			suppliers = supplierGridDao.findAllWithAssignedUser(
					validateSearchFilters(filters, SupplierGrid.class),
					user,
					buildPageable(page, pageLimit, sortColumn, sortDirection),
					SupplierGrid.class
			);
		}

		// For export mode, get ALL records (no pagination)
		if (export) {
			final List<SupplierGridDTO> allData = new ArrayList<>();
			for (final SupplierGrid supplier : suppliers.getContent()) {
				final SupplierGridDTO dto = new SupplierGridDTO(supplier.getId(), supplier.getName(), supplier.getSolutionCount(),
						supplier.getUpdated() == null ? "" : supplier.getUpdated().format(DK_DATE_FORMATTER), supplier.getStatus().getMessage(), allowedActions);
				allData.add(dto);
			}

			excelExportService.exportToExcel(allData, fileName, response);
			return null;
		}

		// Convert to DTO
		final List<SupplierGridDTO> supplierDTOs = new ArrayList<>();
		for (final SupplierGrid supplier : suppliers) {
			final SupplierGridDTO dto = new SupplierGridDTO(
					supplier.getId(),
					supplier.getName(),
					supplier.getSolutionCount(),
					supplier.getUpdated() == null ? "" : supplier.getUpdated().format(DK_DATE_FORMATTER),
					supplier.getStatus().getMessage(),
					allowedActions
			);
			supplierDTOs.add(dto);
		}

		return new PageDTO<>(suppliers.getTotalElements(), supplierDTOs);
	}

	@RequireReadOwnerOnly
    @GetMapping("autocomplete")
    public PageDTO<SupplierDTO> autocomplete(@RequestParam("search") final String search) {
        final Pageable page = PageRequest.of(0, 25, Sort.by("name").ascending());
        if (StringUtils.length(search) == 0) {
            return supplierMapper.toDTO(supplierDao.findAll(page));
        } else {
            return supplierMapper.toDTO(supplierDao.searchForSupplier("%" + search + "%", page));
        }

    }

}

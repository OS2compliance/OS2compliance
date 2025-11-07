package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.SupplierDao;
import dk.digitalidentity.mapping.SupplierMapper;
import dk.digitalidentity.model.ExcelColumn;
import dk.digitalidentity.model.ExcludeFromExport;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.SupplierDTO;
import dk.digitalidentity.model.dto.TagDTO;
import dk.digitalidentity.model.dto.enums.AllowedAction;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.SupplierGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireSupplier;
import dk.digitalidentity.service.SecurityUserService;
import dk.digitalidentity.service.ExcelExportService;
import dk.digitalidentity.service.SupplierService;
import dk.digitalidentity.service.tag.TagService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;

@SuppressWarnings("ClassEscapesDefinedScope")
@Slf4j
@RestController
@RequestMapping("rest/suppliers")
@RequireSupplier
@RequiredArgsConstructor
public class SupplierRestController {
	private final SupplierMapper supplierMapper;
	private final SupplierDao supplierDao;
	private final SupplierService supplierService;
	private final ExcelExportService excelExportService;
	private final SecurityUserService securityUserService;

	record SupplierGridDTO(
			@ExcludeFromExport
			long id,
			@ExcelColumn(headerName = "Navn", order = 1)
			String name,
			@ExcelColumn(headerName = "Antal løsninger", order = 2)
			int solutionCount,
			@ExcelColumn(headerName = "Opdateret", order = 3)
			String updated,
			@ExcelColumn(headerName = "Status", order = 4)
			String status,
			@ExcelColumn(headerName = "Sidste tilsyn", order = 5)
			LocalDate lastOversightDate,
			@ExcludeFromExport
			String kitosUuid,
			@ExcludeFromExport
			List<TagDTO> tags,
			@ExcludeFromExport
			Set<AllowedAction> allowedActions
	) {}

	@RequireReadOwnerOnly
    @PostMapping("list")
	public PageDTO<SupplierGridDTO> list(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "order", required = false) String sortColumn,
			@RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
			@RequestParam Map<String, String> filters // Dynamic filters for search fields
	) {
		User user = securityUserService.getCurrentUserOrThrow();

		Set<AllowedAction> allowedActions = setAllowedActions();

		Page<SupplierGrid> suppliers = supplierService.getSuppliers(sortColumn, sortDirection, filters, page, limit, user);

		Set<Long> entityIds = suppliers.getContent().stream().map(SupplierGrid::getId).collect(Collectors.toSet());
		Map<Long, Tag> tagsById = supplierService.findTagsByEntityIds(entityIds).stream()
				.collect(Collectors.toMap(Tag::getId, t -> t, (a, b) -> b));


		assert suppliers != null;

		// Convert to DTO
		final List<SupplierGridDTO> supplierDTOs = new ArrayList<>();
		for (final SupplierGrid supplier : suppliers) {
			final SupplierGridDTO dto = new SupplierGridDTO(
					supplier.getId(),
					supplier.getName(),
					supplier.getSolutionCount(),
					supplier.getUpdated() == null ? "" : supplier.getUpdated().format(DK_DATE_FORMATTER),
					supplier.getStatus().getMessage(),
					supplier.getLastOversightDate(),
					supplier.getKitosUuid(),
					TagService.toTagDTO(supplier.getTagIds(), tagsById).stream().sorted(Comparator.comparing(TagDTO::getLabel)).toList(),
					allowedActions
			);
			supplierDTOs.add(dto);
		}

		return new PageDTO<>(suppliers.getTotalElements(), supplierDTOs);
	}

	@RequireReadOwnerOnly
	@PostMapping("export")
	public void export(
			@RequestParam(value = "order", required = false) String sortColumn,
			@RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
			@RequestParam(value = "fileName", defaultValue = "export.xlsx") String fileName,
			@RequestParam Map<String, String> filters,
			HttpServletResponse response
	) throws IOException {
		User user = securityUserService.getCurrentUserOrThrow();

		Set<AllowedAction> allowedActions = setAllowedActions();

		int pageLimit = Integer.MAX_VALUE;

		// Fetch all records (no pagination)
		Page<SupplierGrid> suppliers = supplierService.getSuppliers(sortColumn, sortDirection, filters, 0, pageLimit, user);

		Set<Long> entityIds = suppliers.getContent().stream().map(SupplierGrid::getId).collect(Collectors.toSet());
		Map<Long, Tag> tagsById = supplierService.findTagsByEntityIds(entityIds).stream()
				.collect(Collectors.toMap(Tag::getId, t -> t, (a, b) -> b));

		assert suppliers != null;

		final List<SupplierGridDTO> allData = new ArrayList<>();
		for (final SupplierGrid supplier : suppliers.getContent()) {
			final SupplierGridDTO dto = new SupplierGridDTO(supplier.getId(), supplier.getName(), supplier.getSolutionCount(),
					supplier.getUpdated() == null ? "" : supplier.getUpdated().format(DK_DATE_FORMATTER), supplier.getStatus().getMessage(), supplier.getLastOversightDate(), supplier.getKitosUuid(),TagService.toTagDTO(supplier.getTagIds(), tagsById).stream().sorted(Comparator.comparing(TagDTO::getLabel)).toList(), allowedActions);
			allData.add(dto);
		}
		excelExportService.exportToExcel(allData, SupplierGridDTO.class, fileName, response);
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

	private Set<AllowedAction> setAllowedActions() {
		Set<AllowedAction> allowedActions = new HashSet<>();
		if (SecurityUtil.isOperationAllowed(Roles.UPDATE_ALL)) {
			allowedActions.add(AllowedAction.UPDATE);
		}
		if (SecurityUtil.isOperationAllowed(Roles.DELETE_ALL)) {
			allowedActions.add(AllowedAction.DELETE);
		}
		return allowedActions;
	}

}

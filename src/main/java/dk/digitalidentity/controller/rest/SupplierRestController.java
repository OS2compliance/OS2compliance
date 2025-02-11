package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.SupplierDao;
import dk.digitalidentity.dao.grid.SupplierGridDao;
import dk.digitalidentity.mapping.SupplierMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.SupplierDTO;
import dk.digitalidentity.model.entity.grid.SupplierGrid;
import dk.digitalidentity.security.RequireUser;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;

@SuppressWarnings("ClassEscapesDefinedScope")
@Slf4j
@RestController
@RequestMapping("rest/suppliers")
@RequireUser
@RequiredArgsConstructor
public class SupplierRestController {
	private final SupplierGridDao supplierGridDao;
	private final SupplierMapper supplierMapper;
	private final SupplierDao supplierDao;

	record SuppliersPageWrapper(long count, List<SupplierGridDTO> suppliers) {}
	record SupplierGridDTO(long id, String name, int solutionCount, String updated, String status) {}

    @PostMapping("list")
	public SuppliersPageWrapper list(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "limit", defaultValue = "50") int limit,
        @RequestParam(value = "order", required = false) String sortColumn,
        @RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
        @RequestParam Map<String, String> filters // Dynamic filters for search fields
	) {

        // Remove pagination/sorting parameters from the filter map
        filters.remove("page");
        filters.remove("limit");
        filters.remove("order");
        filters.remove("dir");

        //Set sorting
        Sort sort = null;
        if (StringUtils.isNotEmpty(sortColumn) && containsField(sortColumn)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(sortDirection).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, sortColumn);
        } else {
            sort = Sort.unsorted();
        }
        final Pageable sortAndPage = PageRequest.of(page, limit, sort);


        Page<SupplierGrid> suppliers =  supplierGridDao.findAllWithColumnSearch(filters, null, sortAndPage, SupplierGrid.class);

		// Convert to DTO
		final List<SupplierGridDTO> supplierDTOs = new ArrayList<>();

		for (final SupplierGrid supplier : suppliers) {
			final SupplierGridDTO dto = new SupplierGridDTO(supplier.getId(), supplier.getName(), supplier.getSolutionCount(),
					supplier.getUpdated() == null ? "" : supplier.getUpdated().format(DK_DATE_FORMATTER), supplier.getStatus().getMessage());
			supplierDTOs.add(dto);
		}

		return new SuppliersPageWrapper(suppliers.getTotalElements(), supplierDTOs);
	}

    @GetMapping("autocomplete")
    public PageDTO<SupplierDTO> autocomplete(@RequestParam("search") final String search) {
        final Pageable page = PageRequest.of(0, 25, Sort.by("name").ascending());
        if (StringUtils.length(search) == 0) {
            return supplierMapper.toDTO(supplierDao.findAll(page));
        } else {
            return supplierMapper.toDTO(supplierDao.searchForSupplier("%" + search + "%", page));
        }

    }

	private boolean containsField(final String fieldName) {
		return fieldName.equals("updated") || fieldName.equals("name") || fieldName.equals("status") || fieldName.equals("solutionCount");
	}
}

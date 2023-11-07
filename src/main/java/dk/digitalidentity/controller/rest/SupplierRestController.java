package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.SupplierDao;
import dk.digitalidentity.dao.grid.SupplierGridDao;
import dk.digitalidentity.mapping.SupplierMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.SupplierDTO;
import dk.digitalidentity.model.entity.grid.SupplierGrid;
import dk.digitalidentity.security.RequireUser;
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

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;

@Slf4j
@RestController
@RequestMapping("rest/suppliers")
@RequireUser
public class SupplierRestController {
	private final SupplierGridDao supplierGridDao;
	private final SupplierMapper supplierMapper;
	private final SupplierDao supplierDao;

	public SupplierRestController(final SupplierGridDao supplierGridDao, final SupplierDao supplierDao, final SupplierMapper supplierMapper) {
		this.supplierGridDao = supplierGridDao;
		this.supplierDao = supplierDao;
		this.supplierMapper = supplierMapper;
	}

	record SuppliersPageWrapper(long count, List<SupplierGridDTO> suppliers) {}
	record SupplierGridDTO(long id, String name, int solutionCount, String updated, String status) {}

	@PostMapping("list")
	public SuppliersPageWrapper list(
			@RequestParam(name = "search", required = false) final String search,
			@RequestParam(name = "page", required = false) final Integer page,
			@RequestParam(name = "size", required = false) final Integer size,
			@RequestParam(name = "order", required = false) final String order,
			@RequestParam(name = "dir", required = false) final String dir
	) {

		Sort sort = null;
		if (StringUtils.length(order) > 0 && containsField(order)) {
			final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
			sort = Sort.by(direction, order);
		}

		final Pageable sortAndPage = sort != null ?  PageRequest.of(page, size, sort) : PageRequest.of(page, size);

		Page<SupplierGrid> suppliers = null;
		if (StringUtils.length(search) > 0) {
			final List<String> searchableProperties = Arrays.asList("name", "updated", "localizedEnums");
			suppliers = supplierGridDao.findAllCustom(searchableProperties, search, sortAndPage, SupplierGrid.class);
		} else {
			// Fetch paged and sorted
			suppliers = supplierGridDao.findAll(sortAndPage);
		}

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

package dk.digitalidentity.service;

import dk.digitalidentity.dao.SupplierDao;
import dk.digitalidentity.dao.grid.SupplierGridDao;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.SupplierGrid;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

@Service
@Transactional
public class SupplierService {

	public boolean isResponsibleFor(Supplier supplier) {
		return supplier.getResponsibleUser() != null && supplier.getResponsibleUser().getUuid().equals(SecurityUtil.getPrincipalUuid());
	}

	private final SupplierGridDao supplierGridDao;
    private final SupplierDao supplierDao;
	private final RelationService relationService;

	public SupplierService(final SupplierDao supplierDao, RelationService relationService, final SupplierGridDao supplierGridDao) {
        this.supplierDao = supplierDao;
		this.relationService = relationService;
		this.supplierGridDao = supplierGridDao;
	}

    public List<Supplier> getAll() {
        return supplierDao.findAll();
    }

    public Page<Supplier> getPaged(final int pageSize, final int page) {
        return supplierDao.findAll(Pageable.ofSize(pageSize).withPage(page));
    }

    public Supplier create(final Supplier supplier) {
        supplier.getProperties()
            .forEach(p -> p.setEntity(supplier));
        return supplierDao.save(supplier);
    }

    public Optional<Supplier> get(final Long id) {
        return supplierDao.findById(id);
    }

    public Supplier save(final Supplier supplier) {
        return supplierDao.save(supplier);
    }

    public Supplier update(final Supplier supplier) {
        supplier.getProperties()
            .forEach(p -> p.setEntity(supplier));
        return supplierDao.saveAndFlush(supplier);
    }

    public void delete(final Supplier supplier) {
		relationService.deleteRelatedTo(supplier.getId());
        supplierDao.delete(supplier);
    }

    public Optional<Supplier> findByName(final String name) {
        return supplierDao.findByNameIgnoreCase(name);
    }

    public Optional<Supplier> findByCvr(final String cvr) {
        return supplierDao.findFirstByCvr(cvr);
    }

	public Page<SupplierGrid> getSuppliers(String sortColumn, String sortDirection, Map<String, String> filters, int page, int pageLimit, User user) {
		Page<SupplierGrid> suppliers;
		if (SecurityUtil.isOperationAllowed(Roles.READ_ALL)) {
			// Logged-in user can see all
			suppliers = supplierGridDao.findAllWithColumnSearch(
					validateSearchFilters(filters, SupplierGrid.class),
					buildPageable(page, pageLimit, sortColumn, sortDirection),
					SupplierGrid.class
			);
		}
		else {
			// Logged-in user can see only own
			suppliers = supplierGridDao.findAllWithAssignedUser(
					validateSearchFilters(filters, SupplierGrid.class),
					user,
					buildPageable(page, pageLimit, sortColumn, sortDirection),
					SupplierGrid.class
			);
		}
		return suppliers;
	}
}

package dk.digitalidentity.service;

import dk.digitalidentity.dao.SupplierDao;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.security.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SupplierService {

	public boolean isResponsibleFor(Supplier supplier) {
		return  supplier.getResponsibleUser().getUuid().equals(SecurityUtil.getPrincipalUuid());
	}

    private final SupplierDao supplierDao;

    public SupplierService(final SupplierDao supplierDao) {
        this.supplierDao = supplierDao;
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
        supplierDao.delete(supplier);
    }

    public Optional<Supplier> findByName(final String name) {
        return supplierDao.findByNameIgnoreCase(name);
    }

    public Optional<Supplier> findByCvr(final String cvr) {
        return supplierDao.findFirstByCvr(cvr);
    }
}

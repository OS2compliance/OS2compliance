package dk.digitalidentity.integration.cvr;

import dk.digitalidentity.dao.SupplierDao;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.integration.cvr.dto.CvrSearchResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.CVR_UPDATED_PROPERTY;
import static dk.digitalidentity.Constants.NEEDS_CVR_UPDATE_PROPERTY;

@Slf4j
@Service
public class CvrSyncService {
    private final SupplierDao supplierDao;

    public CvrSyncService(final SupplierDao supplierDao) {
        this.supplierDao = supplierDao;
    }

    public void throttle() {
        try {
            Thread.sleep(500);
        } catch (final InterruptedException ignored) {
            throw new RuntimeException("Sync interrupted");
        }
    }

    @Transactional
    public List<String> findCvrsThatNeedsSync() {
        return supplierDao.findByPropertyKeyValue(NEEDS_CVR_UPDATE_PROPERTY, "1").stream()
            .map(Supplier::getCvr)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList());
    }

    @Transactional
    public void updateFromCvr(final String cvr, final CvrSearchResultDTO cvrSearchResultDTO) {
        supplierDao.findByCvr(cvr)
            .ifPresentOrElse(supplier -> {
                supplier.setName(cvrSearchResultDTO.getName());
                supplier.setCity(cvrSearchResultDTO.getCity());
                supplier.setEmail(cvrSearchResultDTO.getEmail());
                supplier.setAddress(cvrSearchResultDTO.getAddress());
                supplier.setPhone(cvrSearchResultDTO.getPhone());
                supplier.setZip(cvrSearchResultDTO.getZipCode());
                supplier.setCountry(cvrSearchResultDTO.getCountry());
                supplier.getProperties().removeIf(p -> p.getKey().equals(NEEDS_CVR_UPDATE_PROPERTY));
                supplier.getProperties().removeIf(p -> p.getKey().equals(CVR_UPDATED_PROPERTY));
                supplier.getProperties().add(Property.builder()
                        .entity(supplier)
                        .key(CVR_UPDATED_PROPERTY)
                        .value(LocalDate.now().toString())
                    .build());
            },
            () -> log.warn("Supplier not found for cvr {}, this should not happen", cvr));
    }

    @Transactional
    public void removeNeedsCvrSync(final String cvr) {
        supplierDao.findByCvr(cvr)
            .ifPresentOrElse(supplier -> {
                    supplier.getProperties().removeIf(p -> p.getKey().equals(NEEDS_CVR_UPDATE_PROPERTY));
                    supplier.getProperties().removeIf(p -> p.getKey().equals(CVR_UPDATED_PROPERTY));
                },
                () -> log.warn("Supplier not found for cvr {}, this should not happen", cvr));
    }

}

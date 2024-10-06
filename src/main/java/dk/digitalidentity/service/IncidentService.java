package dk.digitalidentity.service;

import dk.digitalidentity.dao.IncidentDao;
import dk.digitalidentity.dao.IncidentFieldDao;
import dk.digitalidentity.model.entity.IncidentField;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.IterableUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class IncidentService {
    private final IncidentDao incidentDao;
    private final IncidentFieldDao incidentFieldDao;

    public Optional<IncidentField> findField(final long fieldId) {
        return incidentFieldDao.findById(fieldId);
    }

    public IncidentField save(final IncidentField incidentField) {
        return incidentFieldDao.save(incidentField);
    }

    public long nextIncidentFieldSortKey() {
        return incidentFieldDao.selectMaxSortKey().map(i -> i+1).orElse(0L);
    }

    public List<IncidentField> getAllFields() {
        return IterableUtils.toList(incidentFieldDao.findAllByOrderBySortKeyAsc());
    }

    public void deleteField(final IncidentField incidentField) {
        incidentFieldDao.delete(incidentField);
    }

    public void reorderField(final IncidentField field, boolean down) {
        final List<IncidentField> allFields = incidentFieldDao.findAllByOrderBySortKeyAsc();
        final List<IncidentField> allFieldsReversed = allFields.reversed();
        findPreviousField(field, down ? allFieldsReversed : allFields)
            .ifPresent(f -> {
                // Swap sort keys
                long originalSortKey = field.getSortKey();
                field.setSortKey(f.getSortKey());
                f.setSortKey(originalSortKey);
            });
    }

    private static Optional<IncidentField> findPreviousField(final IncidentField field, final List<IncidentField> allFields) {
        IncidentField lastField = null;
        for (final IncidentField currentField : allFields) {
            if (lastField != null && Objects.equals(currentField.getId(), field.getId())) {
                return Optional.of(lastField);
            }
            lastField = currentField;
        }
        return Optional.empty();
    }
}
